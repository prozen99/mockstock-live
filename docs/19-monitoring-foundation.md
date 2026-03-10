# Monitoring Foundation

## Phase 7 Goal
Phase 7 adds the first explicit observability layer to MockStock Live so the existing trading, quote streaming, and chat flows are measurable during local verification.

This phase does not try to build a full monitoring stack.
It establishes a small, reviewable foundation that makes later performance, load, and concurrency work easier to explain.

---

## What Was Added

- Spring Boot Actuator remains the base operational surface.
- A Prometheus-compatible registry is now registered explicitly for this Boot 4 setup.
- A minimal actuator `prometheus` scrape endpoint was added so local verification can pull metrics with the usual Prometheus text format.
- Custom application metrics were added for the three active runtime flows: trading, quote streaming, and chat.

---

## Why It Matters

- Trading now exposes request volume and validation-failure signals instead of only returning business errors.
- Quote streaming now exposes active subscriber count and publish activity, which makes SSE behavior easier to reason about during local load or browser-based testing.
- Chat now exposes message-send volume and active WebSocket session count, which gives a baseline signal for real-time room activity.
- These metrics give later phases a measurable starting point for concurrency tests, load tests, and operational storytelling.

---

## Exposed Operational Endpoints

- `GET /actuator/health`
  Basic application liveness check.
- `GET /actuator/info`
  Basic operational metadata endpoint.
- `GET /actuator/metrics`
  Lists available meter names.
- `GET /actuator/metrics/{metricName}`
  Reads one metric in actuator JSON form.
- `GET /actuator/prometheus`
  Prometheus-compatible scrape output.
  Request it with `Accept: text/plain`.

---

## Custom Metrics

- `mockstock.trade.requests{type=buy|sell}`
  Counts handled trade requests by trade type.
- `mockstock.trade.validation.failures{type,reason}`
  Counts service-level trade validation failures such as `insufficient_cash`, `no_holding`, and `insufficient_quantity`.
- `mockstock.quote.publish.cycles`
  Counts quote publish passes through the quote stream service.
- `mockstock.quote.snapshots.sent`
  Counts initial SSE snapshot sends.
- `mockstock.quote.publish.failures{stage=snapshot|broadcast}`
  Counts quote delivery failures by stage.
- `mockstock.quote.subscriptions.active`
  Gauge for active SSE quote subscriptions.
- `mockstock.chat.messages.sent`
  Counts chat messages published to room subscribers.
- `mockstock.chat.websocket.sessions.active`
  Gauge for active WebSocket chat sessions.

---

## Local Verification

### 1. Start the application
Run the app with the local profile as usual.

If the current shell does not have Java configured, set:

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

### 2. Verify actuator basics

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/actuator/metrics
```

### 3. Produce metric activity

- Execute a normal buy request.
- Execute an intentionally invalid sell or buy request to trigger a validation failure.
- Open one or more SSE quote stream connections.
- Connect a WebSocket chat client and send a message.

### 4. Read custom metrics

```powershell
Invoke-WebRequest "http://localhost:8080/actuator/metrics/mockstock.trade.requests?tag=type:buy"
Invoke-WebRequest "http://localhost:8080/actuator/metrics/mockstock.trade.validation.failures?tag=type:sell&tag=reason:insufficient_quantity"
Invoke-WebRequest "http://localhost:8080/actuator/metrics/mockstock.quote.subscriptions.active"
Invoke-WebRequest "http://localhost:8080/actuator/metrics/mockstock.chat.messages.sent"
Invoke-WebRequest -Headers @{Accept='text/plain'} http://localhost:8080/actuator/prometheus
```

---

## Example Observations

- After `POST /api/v1/trades/buy`, `mockstock.trade.requests{type="buy"}` increases.
- If a sell request tries to sell more than the current holding quantity, `mockstock.trade.validation.failures{type="sell",reason="insufficient_quantity"}` increases.
- Opening a new SSE quote stream increases `mockstock.quote.subscriptions.active`.
- Each quote publish pass increases `mockstock.quote.publish.cycles`.
- Sending a chat message increases `mockstock.chat.messages.sent`.
- Opening and closing WebSocket chat sessions changes `mockstock.chat.websocket.sessions.active`.

---

## Remaining Gaps / Limitations

- This phase does not add dashboards yet.
- It does not add latency timers or percentile histograms for trading, chat, or quote publish work.
- It does not expose per-room chat subscription counts or unread-count metrics.
- It does not add database query timing meters beyond what the standard stack already provides.
- It does not add distributed tracing, log aggregation, or external monitoring infrastructure.

---

## How This Supports Later Work

- Phase 8 and later load or concurrency work can now correlate failures with concrete request and publish counters.
- The project can now show not only that real-time features exist, but that their runtime activity is visible.
- Future performance or correctness case studies can reference these meters as supporting evidence instead of relying only on manual observation.

---

## Interview-Ready Summary

Phase 7 added the first observability baseline to the project by exposing actuator endpoints, adding Prometheus-compatible scraping, and instrumenting the three most important runtime flows: trading, SSE quote streaming, and chat.

The result is intentionally small but portfolio-relevant: the system now exposes request counts, validation failures, publish cycles, active SSE subscriptions, active WebSocket sessions, and chat message volume, all of which can be verified locally and reused in later performance and concurrency stories.
