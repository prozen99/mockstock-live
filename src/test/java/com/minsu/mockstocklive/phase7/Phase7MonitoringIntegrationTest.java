package com.minsu.mockstocklive.phase7;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.chat.domain.ChatRoom;
import com.minsu.mockstocklive.chat.domain.ChatRoomMember;
import com.minsu.mockstocklive.chat.dto.ChatMessageRequest;
import com.minsu.mockstocklive.chat.repository.ChatRoomMemberRepository;
import com.minsu.mockstocklive.chat.repository.ChatRoomRepository;
import com.minsu.mockstocklive.chat.service.ChatService;
import com.minsu.mockstocklive.exception.BusinessValidationException;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import com.minsu.mockstocklive.stock.service.QuoteStreamService;
import com.minsu.mockstocklive.trading.dto.TradeRequest;
import com.minsu.mockstocklive.trading.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.task.scheduling.enabled=false"
)
@Transactional
class Phase7MonitoringIntegrationTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private QuoteStreamService quoteStreamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @LocalServerPort
    private int port;

    @Test
    void actuatorExposesPhase7CustomMetrics() {
        User user = createUser();
        Stock stock = createStock();
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(stock, stock.getName() + " room"));
        chatRoomMemberRepository.save(ChatRoomMember.create(room, user));

        tradingService.buy(new TradeRequest(user.getId(), stock.getId(), 1L));
        assertThatThrownBy(() -> tradingService.sell(new TradeRequest(user.getId(), stock.getId(), 2L)))
                .isInstanceOf(BusinessValidationException.class);

        chatService.sendMessage(room.getId(), new ChatMessageRequest(user.getId(), "phase7 metric message"));
        quoteStreamService.subscribe(stock.getSymbol());
        quoteStreamService.publishQuotes(stockRepository.findAllByOrderBySymbolAsc());

        RestClient client = RestClient.create("http://localhost:" + port);

        String health = getBody(client, "/actuator/health");
        String tradeRequests = getBody(client, "/actuator/metrics/mockstock.trade.requests?tag=type:buy");
        String tradeFailures = getBody(
                client,
                "/actuator/metrics/mockstock.trade.validation.failures?tag=type:sell&tag=reason:insufficient_quantity"
        );
        String activeSubscriptions = getBody(client, "/actuator/metrics/mockstock.quote.subscriptions.active");
        String chatMessages = getBody(client, "/actuator/metrics/mockstock.chat.messages.sent");
        String prometheus = getBody(client, "/actuator/prometheus", MediaType.TEXT_PLAIN);

        assertThat(health).contains("\"status\":\"UP\"");
        assertThat(tradeRequests).contains("\"name\":\"mockstock.trade.requests\"");
        assertThat(tradeFailures).contains("\"name\":\"mockstock.trade.validation.failures\"");
        assertThat(activeSubscriptions).contains("\"name\":\"mockstock.quote.subscriptions.active\"");
        assertThat(chatMessages).contains("\"name\":\"mockstock.chat.messages.sent\"");
        assertThat(prometheus).contains("mockstock_trade_requests_total");
        assertThat(prometheus).contains("mockstock_quote_subscriptions_active");
        assertThat(prometheus).contains("mockstock_chat_messages_sent_total");
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

    private User createUser() {
        String unique = "phase7-" + UUID.randomUUID();
        return userRepository.save(User.create(
                unique + "@mockstock.live",
                "hashed-password",
                unique.substring(0, Math.min(unique.length(), 30)),
                new BigDecimal("1000000.00")
        ));
    }

    private Stock createStock() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return stockRepository.save(Stock.create(
                "P7" + unique,
                "Phase7 Stock",
                "KOSPI",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO
        ));
    }
}
