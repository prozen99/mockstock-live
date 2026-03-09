package com.minsu.mockstocklive.stock.service;

import com.minsu.mockstocklive.stock.domain.Stock;
import com.minsu.mockstocklive.stock.dto.QuoteResponse;
import com.minsu.mockstocklive.stock.dto.QuoteStreamResponse;
import com.minsu.mockstocklive.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class QuoteStreamService {

    private static final long SSE_TIMEOUT_MILLIS = 0L;

    private final StockRepository stockRepository;
    private final List<QuoteSubscription> subscriptions = new CopyOnWriteArrayList<>();

    public QuoteStreamService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public SseEmitter subscribe(String symbols) {
        Set<String> symbolFilter = normalizeSymbols(symbols);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        QuoteSubscription subscription = new QuoteSubscription(emitter, symbolFilter);
        subscriptions.add(subscription);

        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> {
            emitter.complete();
            subscriptions.remove(subscription);
        });
        emitter.onError(exception -> subscriptions.remove(subscription));

        sendSnapshot(subscription);
        return emitter;
    }

    public void publishQuotes(List<Stock> stocks) {
        if (subscriptions.isEmpty()) {
            return;
        }

        List<QuoteResponse> quoteResponses = stocks.stream()
                .map(this::toQuoteResponse)
                .toList();

        List<QuoteSubscription> expiredSubscriptions = new ArrayList<>();
        for (QuoteSubscription subscription : subscriptions) {
            List<QuoteResponse> filteredQuotes = filterQuotes(quoteResponses, subscription.symbolFilter());
            if (filteredQuotes.isEmpty()) {
                continue;
            }

            try {
                subscription.emitter().send(SseEmitter.event()
                        .name("quote")
                        .data(new QuoteStreamResponse(LocalDateTime.now(), filteredQuotes)));
            } catch (IOException exception) {
                subscription.emitter().completeWithError(exception);
                expiredSubscriptions.add(subscription);
            }
        }

        subscriptions.removeAll(expiredSubscriptions);
    }

    private void sendSnapshot(QuoteSubscription subscription) {
        List<Stock> stocks = subscription.symbolFilter().isEmpty()
                ? stockRepository.findAllByOrderBySymbolAsc()
                : stockRepository.findBySymbolInOrderBySymbolAsc(new ArrayList<>(subscription.symbolFilter()));

        List<QuoteResponse> quoteResponses = stocks.stream()
                .map(this::toQuoteResponse)
                .toList();

        try {
            subscription.emitter().send(SseEmitter.event()
                    .name("quote-snapshot")
                    .data(new QuoteStreamResponse(LocalDateTime.now(), quoteResponses)));
        } catch (IOException exception) {
            subscription.emitter().completeWithError(exception);
            subscriptions.remove(subscription);
        }
    }

    private List<QuoteResponse> filterQuotes(List<QuoteResponse> quotes, Set<String> symbolFilter) {
        if (symbolFilter.isEmpty()) {
            return quotes;
        }

        return quotes.stream()
                .filter(quote -> symbolFilter.contains(quote.symbol()))
                .toList();
    }

    private Set<String> normalizeSymbols(String symbols) {
        if (symbols == null || symbols.isBlank()) {
            return Set.of();
        }

        return List.of(symbols.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private QuoteResponse toQuoteResponse(Stock stock) {
        return new QuoteResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getCurrentPrice(),
                stock.getPriceChangeRate(),
                stock.getUpdatedAt()
        );
    }

    private record QuoteSubscription(
            SseEmitter emitter,
            Set<String> symbolFilter
    ) {
    }
}
