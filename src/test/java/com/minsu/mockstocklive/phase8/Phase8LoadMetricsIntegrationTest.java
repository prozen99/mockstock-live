package com.minsu.mockstocklive.phase8;

import com.minsu.mockstocklive.auth.domain.User;
import com.minsu.mockstocklive.auth.repository.UserRepository;
import com.minsu.mockstocklive.portfolio.service.PortfolioService;
import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import com.minsu.mockstocklive.stock.service.StockService;
import com.minsu.mockstocklive.trading.dto.TradeRequest;
import com.minsu.mockstocklive.trading.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.task.scheduling.enabled=false"
)
@Transactional
class Phase8LoadMetricsIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @LocalServerPort
    private int port;

    @Test
    void actuatorExposesPhase8ReadMetrics() {
        User user = createUser();
        Stock stock = createStock();

        stockService.getStocks();
        tradingService.buy(new TradeRequest(user.getId(), stock.getId(), 1L));
        portfolioService.getHoldings(user.getId());
        tradingService.getTradeHistory(user.getId(), 0, 20);
        tradingService.getTradeHistoryByCursor(user.getId(), null, 20);

        RestClient client = RestClient.create("http://localhost:" + port);

        String readRequests = getBody(client, "/actuator/metrics/mockstock.read.requests?tag=flow:stock_list");
        String holdingsReads = getBody(client, "/actuator/metrics/mockstock.read.requests?tag=flow:holdings_list");
        String offsetLatency = getBody(client, "/actuator/metrics/mockstock.read.latency?tag=flow:trade_history_offset");
        String cursorLatency = getBody(client, "/actuator/metrics/mockstock.read.latency?tag=flow:trade_history_cursor");
        String prometheus = getBody(client, "/actuator/prometheus", MediaType.TEXT_PLAIN);

        assertThat(readRequests).contains("\"name\":\"mockstock.read.requests\"");
        assertThat(holdingsReads).contains("\"name\":\"mockstock.read.requests\"");
        assertThat(offsetLatency).contains("\"name\":\"mockstock.read.latency\"");
        assertThat(cursorLatency).contains("\"name\":\"mockstock.read.latency\"");
        assertThat(prometheus).contains("mockstock_read_requests_total");
        assertThat(prometheus).contains("flow=\"stock_list\"");
        assertThat(prometheus).contains("flow=\"holdings_list\"");
        assertThat(prometheus).contains("mockstock_read_latency_seconds_count");
        assertThat(prometheus).contains("flow=\"trade_history_offset\"");
        assertThat(prometheus).contains("flow=\"trade_history_cursor\"");
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
        String unique = "phase8-" + UUID.randomUUID();
        return userRepository.save(User.create(
                unique + "@mockstock.live",
                "hashed-password",
                unique.substring(0, Math.min(unique.length(), 30)),
                new BigDecimal("1000000.00")
        ));
    }

    private Stock createStock() {
        String symbol = "P8" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return stockRepository.save(Stock.create(
                symbol,
                "Phase8 Stock",
                "KOSPI",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO
        ));
    }
}
