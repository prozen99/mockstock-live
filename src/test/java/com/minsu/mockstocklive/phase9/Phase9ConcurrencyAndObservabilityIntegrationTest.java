package com.minsu.mockstocklive.phase9;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.domain.ChatRoomMember;
import com.minsu.mockstocklive.chat.dto.ChatMessageRequest;
import com.minsu.mockstocklive.chat.repository.ChatRoomMemberRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomRepository;
import com.minsu.mockstocklive.chat.service.ChatService;
import com.minsu.mockstocklive.exception.BusinessValidationException;
import com.minsu.mockstocklive.monitoring.ChatRoomSubscriptionMetrics;
import com.minsu.mockstocklive.portfolio.domain.Holding;
import com.minsu.mockstocklive.portfolio.repository.HoldingRepository;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import com.minsu.mockstocklive.stock.service.QuoteStreamService;
import com.minsu.mockstocklive.trading.domain.TradeOrder;
import com.minsu.mockstocklive.trading.domain.TradeType;
import com.minsu.mockstocklive.trading.dto.TradeRequest;
import com.minsu.mockstocklive.trading.repository.TradeOrderRepository;
import com.minsu.mockstocklive.trading.service.TradingService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.task.scheduling.enabled=false"
)
class Phase9ConcurrencyAndObservabilityIntegrationTest {

    @Autowired
    private QuoteStreamService quoteStreamService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatRoomSubscriptionMetrics chatRoomSubscriptionMetrics;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    @LocalServerPort
    private int port;

    Phase9ConcurrencyAndObservabilityIntegrationTest(@Autowired PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void actuatorExposesPhase9RealTimeMetrics() throws Exception {
        Stock quoteStock = createStock("P9Q", "Phase9 Quote Stock", new BigDecimal("1300.00"));
        List<InputStream> quoteStreams = new ArrayList<>();

        try {
            quoteStreams.addAll(openQuoteStreams(quoteStock.getSymbol(), 6));
            awaitGaugeValue("mockstock.quote.subscriptions.active", 6.0);

            for (int index = 0; index < 4; index++) {
                quoteStreamService.publishQuotes(List.of(quoteStock));
            }

            User user = createUser(new BigDecimal("1000000.00"));
            ChatRoom hotRoom = chatRoomRepository.save(ChatRoom.create(
                    createStock("P9H", "Phase9 Hot Room Stock", new BigDecimal("900.00")),
                    "Phase9 Hot Room"
            ));
            ChatRoom quietRoom = chatRoomRepository.save(ChatRoom.create(
                    createStock("P9C", "Phase9 Cold Room Stock", new BigDecimal("800.00")),
                    "Phase9 Quiet Room"
            ));
            chatRoomMemberRepository.save(ChatRoomMember.create(hotRoom, user));

            chatRoomSubscriptionMetrics.subscribe("phase9-room-s1", "sub-1", "/sub/chat/rooms/" + hotRoom.getId());
            chatRoomSubscriptionMetrics.subscribe("phase9-room-s2", "sub-1", "/sub/chat/rooms/" + hotRoom.getId());
            chatRoomSubscriptionMetrics.subscribe("phase9-room-s3", "sub-1", "/sub/chat/rooms/" + hotRoom.getId());
            chatRoomSubscriptionMetrics.subscribe("phase9-room-s4", "sub-1", "/sub/chat/rooms/" + quietRoom.getId());

            chatService.sendMessage(hotRoom.getId(), new ChatMessageRequest(user.getId(), "phase9 room message 1"));
            chatService.sendMessage(hotRoom.getId(), new ChatMessageRequest(user.getId(), "phase9 room message 2"));

            RestClient client = RestClient.create("http://localhost:" + port);

            String quoteEventsSent = getBody(client, "/actuator/metrics/mockstock.quote.events.sent");
            String quoteRecipients = getBody(client, "/actuator/metrics/mockstock.quote.publish.recipients");
            String quoteLatency = getBody(client, "/actuator/metrics/mockstock.quote.publish.latency");
            String hotRoomSubscriptions = getBody(
                    client,
                    "/actuator/metrics/mockstock.chat.room.subscriptions.active?tag=roomId:" + hotRoom.getId()
            );
            String quietRoomSubscriptions = getBody(
                    client,
                    "/actuator/metrics/mockstock.chat.room.subscriptions.active?tag=roomId:" + quietRoom.getId()
            );
            String chatLatency = getBody(client, "/actuator/metrics/mockstock.chat.send.latency");
            String prometheus = getBody(client, "/actuator/prometheus", MediaType.TEXT_PLAIN);

            assertThat(quoteEventsSent).contains("\"name\":\"mockstock.quote.events.sent\"");
            assertThat(quoteRecipients).contains("\"name\":\"mockstock.quote.publish.recipients\"");
            assertThat(quoteLatency).contains("\"name\":\"mockstock.quote.publish.latency\"");
            assertThat(hotRoomSubscriptions).contains("\"name\":\"mockstock.chat.room.subscriptions.active\"");
            assertThat(quietRoomSubscriptions).contains("\"name\":\"mockstock.chat.room.subscriptions.active\"");
            assertThat(chatLatency).contains("\"name\":\"mockstock.chat.send.latency\"");
            assertThat(prometheus).contains("mockstock_quote_events_sent_total");
            assertThat(prometheus).contains("mockstock_quote_publish_recipients");
            assertThat(prometheus).contains("mockstock_quote_publish_latency_seconds_bucket");
            assertThat(prometheus).contains("mockstock_chat_send_latency_seconds_bucket");
            assertThat(prometheus).contains("mockstock_chat_room_subscriptions_active{roomId=\"" + hotRoom.getId() + "\"}");
            assertThat(prometheus).contains("mockstock_chat_room_subscriptions_active{roomId=\"" + quietRoom.getId() + "\"}");

            double deliveredEvents = counterValue("mockstock.quote.events.sent");
            DistributionSummary recipients = meterRegistry.get("mockstock.quote.publish.recipients").summary();
            Timer quoteLatencyTimer = meterRegistry.get("mockstock.quote.publish.latency").timer();
            Timer chatLatencyTimer = meterRegistry.get("mockstock.chat.send.latency").timer();
            double hotRoomCount = gaugeValue("mockstock.chat.room.subscriptions.active", "roomId", hotRoom.getId().toString());
            double quietRoomCount = gaugeValue("mockstock.chat.room.subscriptions.active", "roomId", quietRoom.getId().toString());

            assertThat(deliveredEvents).isEqualTo(24.0);
            assertThat(recipients.count()).isEqualTo(4L);
            assertThat(recipients.totalAmount()).isEqualTo(24.0);
            assertThat(quoteLatencyTimer.count()).isEqualTo(4L);
            assertThat(chatLatencyTimer.count()).isGreaterThanOrEqualTo(2L);
            assertThat(hotRoomCount).isEqualTo(3.0);
            assertThat(quietRoomCount).isEqualTo(1.0);

            System.out.println(
                    "PHASE9_REALTIME afterSubscribers=6 beforeVisibleMetrics=2 afterDeliveredEvents=24 " +
                            "publishCycles=4 publishRecipientTotal=24 hotRoomSubscriptions=3 quietRoomSubscriptions=1 " +
                            "chatSendCount=" + chatLatencyTimer.count() + " quoteLatencyMaxMs=" +
                            roundMillis(quoteLatencyTimer.max(TimeUnit.MILLISECONDS)) + " chatLatencyMaxMs=" +
                            roundMillis(chatLatencyTimer.max(TimeUnit.MILLISECONDS))
            );
        } finally {
            quoteStreams.forEach(this::closeQuietly);
            chatRoomSubscriptionMetrics.disconnect("phase9-room-s1");
            chatRoomSubscriptionMetrics.disconnect("phase9-room-s2");
            chatRoomSubscriptionMetrics.disconnect("phase9-room-s3");
            chatRoomSubscriptionMetrics.disconnect("phase9-room-s4");
        }
    }

    @Test
    void concurrentBuyBaselineFailsButLockedServiceStaysConsistent() throws Exception {
        Stock stock = createStock("P9T", "Phase9 Trade Stock", new BigDecimal("700.00"));
        User baselineUser = createUser(new BigDecimal("1000.00"));
        User lockedUser = createUser(new BigDecimal("1000.00"));
        holdingRepository.save(Holding.create(baselineUser, stock, 1L, stock.getCurrentPrice()));
        holdingRepository.save(Holding.create(lockedUser, stock, 1L, stock.getCurrentPrice()));

        BuyRaceResult baseline = runUnlockedConcurrentBuyBaseline(baselineUser.getId(), stock.getId());
        BuyRaceResult hardened = runLockedConcurrentBuys(lockedUser.getId(), stock.getId());

        assertThat(baseline.successCount()).isEqualTo(2);
        assertThat(baseline.failureCount()).isEqualTo(0);
        assertThat(baseline.tradeOrderCount()).isEqualTo(2);
        assertThat(baseline.holdingRowCount()).isEqualTo(1);
        assertThat(baseline.totalHoldingQuantity()).isEqualTo(2L);
        assertThat(baseline.finalCashBalance()).isEqualByComparingTo("300.00");

        assertThat(hardened.successCount()).isEqualTo(1);
        assertThat(hardened.failureCount()).isEqualTo(1);
        assertThat(hardened.tradeOrderCount()).isEqualTo(1);
        assertThat(hardened.holdingRowCount()).isEqualTo(1);
        assertThat(hardened.totalHoldingQuantity()).isEqualTo(2L);
        assertThat(hardened.finalCashBalance()).isEqualByComparingTo("300.00");
        assertThat(hardened.failureTypes()).containsExactly("BusinessValidationException");

        System.out.println(
                "PHASE9_CONCURRENT_BUY baselineSuccess=2 baselineOrders=2 baselineHoldingRows=1 " +
                        "baselineHoldingQuantity=2 baselineFinalCash=300.00 hardenedSuccess=1 hardenedFailures=1 " +
                        "hardenedOrders=1 hardenedHoldingRows=1 hardenedHoldingQuantity=2 hardenedFinalCash=300.00"
        );
    }

    private List<InputStream> openQuoteStreams(String symbol, int count) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        List<InputStream> streams = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/quotes/stream?symbols=" + symbol))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            streams.add(response.body());
        }

        return streams;
    }

    private BuyRaceResult runUnlockedConcurrentBuyBaseline(Long userId, Long stockId) throws Exception {
        CyclicBarrier balanceBarrier = new CyclicBarrier(2);
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
        Holding baselineHolding = holdingRepository.findByUserIdAndStockId(userId, stockId).orElseThrow();
        int successCount = executeConcurrentTasks(() -> () -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    Stock stock = stockRepository.findById(stockId).orElseThrow();
                    BigDecimal totalAmount = stock.getCurrentPrice();
                    long currentQuantity = holdingRepository.findById(baselineHolding.getId()).orElseThrow().getQuantity();

                    if (user.getCashBalance().compareTo(totalAmount) < 0) {
                        throw new BusinessValidationException("Insufficient cash balance");
                    }

                    awaitBarrier(balanceBarrier);
                    BigDecimal updatedCashBalance = user.getCashBalance().subtract(totalAmount);
                    long updatedQuantity = currentQuantity + 1L;
                    LocalDateTime now = LocalDateTime.now();

                    jdbcTemplate.update(
                            "update users set cash_balance = ?, updated_at = ? where id = ?",
                            updatedCashBalance,
                            now,
                            userId
                    );
                    jdbcTemplate.update(
                            "update holdings set quantity = ?, avg_buy_price = ?, evaluated_at = ? where id = ?",
                            updatedQuantity,
                            stock.getCurrentPrice(),
                            now,
                            baselineHolding.getId()
                    );
                    tradeOrderRepository.save(TradeOrder.create(user, stock, TradeType.BUY, stock.getCurrentPrice(), 1L));
                });
                return true;
            } catch (Exception exception) {
                failures.add(rootCause(exception).getClass().getSimpleName());
                return false;
            }
        });

        return readBuyRaceResult(userId, successCount, failures);
    }

    private BuyRaceResult runLockedConcurrentBuys(Long userId, Long stockId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
        int successCount = executeConcurrentTasks(() -> () -> {
            ready.countDown();
            ready.await(5, TimeUnit.SECONDS);
            start.await(5, TimeUnit.SECONDS);

            try {
                tradingService.buy(new TradeRequest(userId, stockId, 1L));
                return true;
            } catch (Exception exception) {
                failures.add(rootCause(exception).getClass().getSimpleName());
                return false;
            }
        }, start);

        return readBuyRaceResult(userId, successCount, failures);
    }

    private int executeConcurrentTasks(Callable<Callable<Boolean>> callableFactory) throws Exception {
        return executeConcurrentTasks(callableFactory, null);
    }

    private int executeConcurrentTasks(Callable<Callable<Boolean>> callableFactory, CountDownLatch starter) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures = List.of(
                    executor.submit(callableFactory.call()),
                    executor.submit(callableFactory.call())
            );

            if (starter != null) {
                starter.countDown();
            }

            int successCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }
            return successCount;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private BuyRaceResult readBuyRaceResult(Long userId, int successCount, ConcurrentLinkedQueue<String> failures) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Holding> holdings = holdingRepository.findByUserIdOrderByIdAsc(userId);
        long totalHoldingQuantity = holdings.stream().mapToLong(Holding::getQuantity).sum();

        return new BuyRaceResult(
                successCount,
                failures.size(),
                List.copyOf(failures),
                tradeOrderRepository.countByUserId(userId),
                holdings.size(),
                totalHoldingQuantity,
                user.getCashBalance()
        );
    }

    private void awaitGaugeValue(String metricName, double expectedValue) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {
            if (Double.compare(gaugeValue(metricName), expectedValue) == 0) {
                return;
            }
            Thread.sleep(100);
        }

        assertThat(gaugeValue(metricName)).isEqualTo(expectedValue);
    }

    private double counterValue(String metricName) {
        return meterRegistry.find(metricName)
                .counter()
                .count();
    }

    private double gaugeValue(String metricName) {
        return meterRegistry.find(metricName)
                .gauge()
                .value();
    }

    private double gaugeValue(String metricName, String tagKey, String tagValue) {
        return meterRegistry.find(metricName)
                .tag(tagKey, tagValue)
                .gauge()
                .value();
    }

    private String getBody(RestClient client, String path) {
        return getBody(client, path, MediaType.APPLICATION_JSON);
    }

    private String getBody(RestClient client, String path, MediaType acceptType) {
        return client.get()
                .uri(path)
                .accept(acceptType)
                .exchange((request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException(
                                "Unexpected status: " + response.getStatusCode() + " body=" + body
                        );
                    }
                    return body;
                });
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Barrier synchronization failed", exception);
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (Exception ignored) {
        }
    }

    private double roundMillis(double milliseconds) {
        return Math.round(milliseconds * 100.0) / 100.0;
    }

    private User createUser(BigDecimal cashBalance) {
        String unique = "phase9-" + UUID.randomUUID();
        return userRepository.save(User.create(
                unique + "@mockstock.live",
                "hashed-password",
                unique.substring(0, Math.min(unique.length(), 30)),
                cashBalance
        ));
    }

    private Stock createStock(String prefix, String name, BigDecimal currentPrice) {
        String symbol = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return stockRepository.save(Stock.create(
                symbol,
                name,
                "KOSPI",
                currentPrice,
                BigDecimal.ZERO
        ));
    }

    private record BuyRaceResult(
            int successCount,
            int failureCount,
            List<String> failureTypes,
            long tradeOrderCount,
            int holdingRowCount,
            long totalHoldingQuantity,
            BigDecimal finalCashBalance
    ) {
    }
}
