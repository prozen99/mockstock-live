package com.minsu.mockstocklive.monitoring;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "prometheus")
public class PrometheusEndpoint {

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public PrometheusEndpoint(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @ReadOperation(produces = "text/plain;version=0.0.4;charset=utf-8")
    public String scrape() {
        return prometheusMeterRegistry.scrape();
    }
}
