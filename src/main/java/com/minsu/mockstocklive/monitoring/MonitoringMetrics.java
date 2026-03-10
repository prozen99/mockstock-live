package com.minsu.mockstocklive.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MonitoringMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter buyTradeRequests;
    private final Counter sellTradeRequests;
    private final Counter quotePublishCycles;
    private final Counter quoteSnapshotsSent;
    private final Counter chatMessagesSent;

    public MonitoringMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.buyTradeRequests = Counter.builder("mockstock.trade.requests")
                .description("Number of trade requests handled by the trading service")
                .tag("type", "buy")
                .register(meterRegistry);
        this.sellTradeRequests = Counter.builder("mockstock.trade.requests")
                .description("Number of trade requests handled by the trading service")
                .tag("type", "sell")
                .register(meterRegistry);
        this.quotePublishCycles = Counter.builder("mockstock.quote.publish.cycles")
                .description("Number of quote publish cycles executed")
                .register(meterRegistry);
        this.quoteSnapshotsSent = Counter.builder("mockstock.quote.snapshots.sent")
                .description("Number of initial SSE quote snapshots sent")
                .register(meterRegistry);
        this.chatMessagesSent = Counter.builder("mockstock.chat.messages.sent")
                .description("Number of chat messages broadcast to rooms")
                .register(meterRegistry);
    }

    public void recordTradeRequest(String type) {
        if ("buy".equals(type)) {
            buyTradeRequests.increment();
            return;
        }

        if ("sell".equals(type)) {
            sellTradeRequests.increment();
        }
    }

    public void recordTradeValidationFailure(String type, String reason) {
        meterRegistry.counter(
                "mockstock.trade.validation.failures",
                "type", type,
                "reason", reason
        ).increment();
    }

    public void recordQuotePublishCycle() {
        quotePublishCycles.increment();
    }

    public void recordQuoteSnapshotSent() {
        quoteSnapshotsSent.increment();
    }

    public void recordQuotePublishFailure(String stage) {
        meterRegistry.counter(
                "mockstock.quote.publish.failures",
                "stage", stage
        ).increment();
    }

    public void recordChatMessageSent() {
        chatMessagesSent.increment();
    }

    public <T> T recordRead(String flow, Supplier<T> supplier) {
        meterRegistry.counter("mockstock.read.requests", "flow", flow).increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            return supplier.get();
        } finally {
            sample.stop(Timer.builder("mockstock.read.latency")
                    .description("Latency of selected read-heavy flows")
                    .tag("flow", flow)
                    .register(meterRegistry));
        }
    }
}
