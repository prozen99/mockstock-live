package com.minsu.mockstocklive.phase6;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.chat.domain.ChatMessage;
import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.domain.ChatRoomMember;
import com.minsu.mockstocklive.chat.dto.ChatRoomResponse;
import com.minsu.mockstocklive.chat.repository.ChatMessageRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomMemberRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomRepository;
import com.minsu.mockstocklive.chat.service.ChatService;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import com.minsu.mockstocklive.trading.dto.TradeCursorHistoryResponse;
import com.minsu.mockstocklive.trading.dto.TradeHistoryItemResponse;
import com.minsu.mockstocklive.trading.dto.TradeHistoryResponse;
import com.minsu.mockstocklive.trading.service.TradingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class Phase6PerformanceLabIntegrationTest {

    private static final BigDecimal STARTING_CASH = new BigDecimal("100000000.00");

    @Autowired
    private ChatService chatService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void chatRoomListProjectionRemovesRepeatedRoomLookups() {
        User user = createUser("chat");
        seedChatRooms(user, 40);

        List<ChatRoomResponse> baselineRooms = loadLegacyRoomList(user.getId());
        List<ChatRoomResponse> improvedRooms = chatService.getRooms(user.getId());

        assertThat(improvedRooms).containsExactlyElementsOf(baselineRooms);

        long baselineQueries = countQueries(() -> loadLegacyRoomList(user.getId()));
        long improvedQueries = countQueries(() -> chatService.getRooms(user.getId()));
        long baselineAverageMillis = averageMillis(() -> loadLegacyRoomList(user.getId()), 5);
        long improvedAverageMillis = averageMillis(() -> chatService.getRooms(user.getId()), 5);

        assertThat(improvedQueries).isLessThan(baselineQueries);

        System.out.printf(
                "PHASE6_CHAT_ROOM_LIST baselineQueries=%d improvedQueries=%d baselineAvgMs=%d improvedAvgMs=%d roomCount=%d%n",
                baselineQueries,
                improvedQueries,
                baselineAverageMillis,
                improvedAverageMillis,
                improvedRooms.size()
        );
    }

    @Test
    void cursorTradeHistoryKeepsResultsButAvoidsDeepOffsetCost() {
        User user = createUser("trade");
        List<Stock> stocks = createStocks("trade", 40);
        seedTradeOrders(user.getId(), stocks, 12_000);

        int page = 300;
        int size = 20;
        long cursorOffset = (long) page * size - 1;
        Long beforeTradeId = jdbcTemplate.queryForObject(
                """
                select id
                from trade_orders
                where user_id = ?
                order by id desc
                limit 1 offset ?
                """,
                Long.class,
                user.getId(),
                cursorOffset
        );

        assertThat(beforeTradeId).isNotNull();

        TradeHistoryResponse legacyPage = tradingService.getTradeHistory(user.getId(), page, size);
        TradeCursorHistoryResponse cursorPage = tradingService.getTradeHistoryByCursor(user.getId(), beforeTradeId, size);

        assertThat(cursorPage.trades())
                .extracting(TradeHistoryItemResponse::tradeOrderId)
                .containsExactlyElementsOf(
                        legacyPage.trades().stream().map(TradeHistoryItemResponse::tradeOrderId).toList()
                );

        long legacyQueries = countQueries(() -> tradingService.getTradeHistory(user.getId(), page, size));
        long cursorQueries = countQueries(() -> tradingService.getTradeHistoryByCursor(user.getId(), beforeTradeId, size));
        long legacyAverageMillis = averageMillis(() -> tradingService.getTradeHistory(user.getId(), page, size), 5);
        long cursorAverageMillis = averageMillis(() -> tradingService.getTradeHistoryByCursor(user.getId(), beforeTradeId, size), 5);
        int legacyExplainRows = explainRows(
                """
                explain
                select id, stock_id, trade_type, price, quantity, total_amount, created_at
                from trade_orders
                where user_id = ?
                order by created_at desc, id desc
                limit ? offset ?
                """,
                user.getId(),
                size,
                (long) page * size
        );
        int cursorExplainRows = explainRows(
                """
                explain
                select id, stock_id, trade_type, price, quantity, total_amount, created_at
                from trade_orders
                where user_id = ? and id < ?
                order by id desc
                limit ?
                """,
                user.getId(),
                beforeTradeId,
                size
        );

        System.out.printf(
                "PHASE6_TRADE_CURSOR baselineQueries=%d improvedQueries=%d baselineAvgMs=%d improvedAvgMs=%d baselineExplainRows=%d improvedExplainRows=%d page=%d size=%d%n",
                legacyQueries,
                cursorQueries,
                legacyAverageMillis,
                cursorAverageMillis,
                legacyExplainRows,
                cursorExplainRows,
                page,
                size
        );

        assertThat(cursorQueries).isLessThan(legacyQueries);
    }

    private User createUser(String prefix) {
        String unique = prefix + "-" + UUID.randomUUID();
        return userRepository.save(User.create(
                unique + "@mockstock.live",
                "hashed-password",
                unique.substring(0, Math.min(unique.length(), 30)),
                STARTING_CASH
        ));
    }

    private List<Stock> createStocks(String prefix, int count) {
        List<Stock> stocks = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            stocks.add(stockRepository.save(Stock.create(
                    (prefix + "S" + UUID.randomUUID()).substring(0, 20),
                    prefix + "-stock-" + index,
                    "KOSPI",
                    new BigDecimal("1000.00").add(BigDecimal.valueOf(index)),
                    BigDecimal.ZERO
            )));
        }

        return stocks;
    }

    private void seedChatRooms(User user, int roomCount) {
        List<Stock> stocks = createStocks("chat", roomCount);

        for (int index = 0; index < roomCount; index++) {
            Stock stock = stocks.get(index);
            ChatRoom room = chatRoomRepository.save(ChatRoom.create(stock, stock.getName() + " room"));
            ChatMessage message = chatMessageRepository.save(ChatMessage.create(room, user, "message-" + index));
            room.updateLastMessage(message);
            chatRoomRepository.save(room);

            if (index % 2 == 0) {
                chatRoomMemberRepository.save(ChatRoomMember.create(room, user));
            }
        }

        flushAndClear();
    }

    private void seedTradeOrders(Long userId, List<Stock> stocks, int tradeCount) {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(1);
        List<Object[]> batchArguments = new ArrayList<>(tradeCount);

        for (int index = 0; index < tradeCount; index++) {
            Stock stock = stocks.get(index % stocks.size());
            batchArguments.add(new Object[]{
                    userId,
                    stock.getId(),
                    "BUY",
                    new BigDecimal("1000.00").add(BigDecimal.valueOf(index % 50)),
                    1L,
                    new BigDecimal("1000.00").add(BigDecimal.valueOf(index % 50)),
                    Timestamp.valueOf(baseTime.plusSeconds(index))
            });
        }

        jdbcTemplate.batchUpdate(
                """
                insert into trade_orders (
                    user_id,
                    stock_id,
                    trade_type,
                    price,
                    quantity,
                    total_amount,
                    created_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                batchArguments
        );

        flushAndClear();
    }

    private List<ChatRoomResponse> loadLegacyRoomList(Long userId) {
        return chatRoomRepository.findAllByOrderByIdAsc().stream()
                .map(room -> {
                    String lastMessagePreview = room.getLastMessageId() == null
                            ? null
                            : chatMessageRepository.findById(room.getLastMessageId())
                            .map(ChatMessage::getContent)
                            .orElse(null);

                    boolean joined = chatRoomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId);

                    return new ChatRoomResponse(
                            room.getId(),
                            room.getStock().getId(),
                            room.getStock().getSymbol(),
                            room.getStock().getName(),
                            room.getRoomName(),
                            room.getLastMessageId(),
                            lastMessagePreview,
                            room.getLastMessageAt(),
                            joined
                    );
                })
                .toList();
    }

    private long countQueries(Supplier<?> supplier) {
        flushAndClear();
        statistics.clear();
        supplier.get();
        return statistics.getPrepareStatementCount();
    }

    private long averageMillis(Runnable action, int repetitions) {
        long totalNanos = 0L;

        for (int index = 0; index < repetitions; index++) {
            flushAndClear();
            long start = System.nanoTime();
            action.run();
            totalNanos += System.nanoTime() - start;
        }

        return totalNanos / repetitions / 1_000_000L;
    }

    private int explainRows(String sql, Object... args) {
        List<Map<String, Object>> explainRows = jdbcTemplate.queryForList(sql, args);
        Object rows = explainRows.get(0).get("rows");
        return ((Number) rows).intValue();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
