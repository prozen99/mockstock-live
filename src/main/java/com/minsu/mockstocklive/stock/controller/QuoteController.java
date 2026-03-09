package com.minsu.mockstocklive.stock.controller;

import com.minsu.mockstocklive.stock.service.QuoteStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteStreamService quoteStreamService;

    public QuoteController(QuoteStreamService quoteStreamService) {
        this.quoteStreamService = quoteStreamService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuotes(@RequestParam(required = false) String symbols) {
        return quoteStreamService.subscribe(symbols);
    }
}
