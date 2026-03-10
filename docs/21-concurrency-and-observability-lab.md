# Concurrency And Observability Lab

## Scope
This combined phase closes two of the highest-value gaps left after Phase 8 without broadening into new infrastructure work:

- Track A: real-time observability for SSE quote fan-out and chat room activity
- Track B: one focused trading correctness risk under concurrent buy requests

All measurements below were reproduced locally on `2026-03-11`.

---

## Selected Targets

### Track A1. SSE fan-out visibility
Why it mattered:
Phase 7 and Phase 8 already exposed active SSE subscriptions and quote publish cycle counts, but they still could not answer the practical question, "how much work did one publish cycle actually do?"

### Track A2. Chat room activity skew
Why it mattered:
The project already exposed total chat message count and total WebSocket sessions, but that still hid hot-room concentration and chat send cost.

### Track B. Concurrent buy correctness
Why it mattered:
The trading flow changes user cash, holdings, and trade history together.
Without serialization for the same user, repeated buy requests could both pass the same balance check and leave the final state impossible.

---

## Reproduction Setup

### Track A baseline setup
- Quote stream setup:
  one stock, `6` live SSE subscribers, `4` explicit publish cycles
- Chat setup:
  one user, one hot room with `3` subscriptions, one quiet room with `1` subscription, `2` messages sent to the hot room
- Measurement method:
  actuator metric endpoints, Prometheus scrape output, and direct `MeterRegistry` assertions in the Phase 9 integration test

### Track B baseline setup
- Data setup:
  one stock priced at `700.00`
  two users with `1000.00` cash each
  one existing holding row per user with quantity `1`
- Request pattern:
  two concurrent buy requests for quantity `1` on the same user and stock
- Measurement method:
  success/failure counts, final cash balance, number of trade orders, holding row count, and final holding quantity

---

## Before Behavior / Results

### Track A before
- SSE visibility before this phase:
  `mockstock.quote.publish.cycles` and `mockstock.quote.subscriptions.active` existed, but delivered fan-out and publish latency were not visible.
- Chat visibility before this phase:
  `mockstock.chat.messages.sent` and `mockstock.chat.websocket.sessions.active` existed, but room-level subscription skew and chat send latency were not visible.

### Track B before
The old trade flow used a normal `findById` lookup for the user and then performed a read-modify-write update inside the transaction.
That shape allowed both concurrent buyers to read the same `1000.00` balance before either write became visible.

Baseline result reproduced by the Phase 9 test:

| Measure | Baseline result |
| --- | ---: |
| Successful concurrent buys | 2 |
| Failed concurrent buys | 0 |
| Trade orders saved | 2 |
| Holding rows | 1 |
| Final holding quantity | 2 |
| Final cash balance | 300.00 |

Why that is wrong:
two buy orders at `700.00` each should require `1400.00`, but both requests succeeded and only one effective cash deduction / holding increment survived.
That leaves order history, holdings, and cash balance out of sync.

---

## Root Cause

### Track A root cause
The monitoring foundation had count-level visibility, but not workload-shape visibility.
It could show that quote publishing happened, not how many subscriber deliveries each cycle performed or how long the publish work took.

### Track B root cause
The trade flow did not serialize same-user buy requests.
Two transactions could both pass the balance check against stale user state before either commit completed.

---

## Minimal Changes Applied

### Track A changes
- Added `mockstock.quote.events.sent`
  counter for delivered SSE quote events
- Added `mockstock.quote.publish.recipients`
  distribution summary for recipients per publish cycle
- Added `mockstock.quote.publish.latency`
  timer with histogram buckets for publish-cycle cost
- Added `mockstock.chat.send.latency`
  timer with histogram buckets for successful chat message processing
- Added `mockstock.chat.room.subscriptions.active{roomId}`
  per-room gauge driven by STOMP subscribe, unsubscribe, and disconnect events

Code changes stayed small and local:
- quote instrumentation in `QuoteStreamService`
- chat instrumentation in `ChatService`
- room-subscription tracking in `ChatRoomSubscriptionMetrics`
- shared meter registration in `MonitoringMetrics`

### Track B changes
- Added `UserRepository.findByIdForUpdate(...)` with `PESSIMISTIC_WRITE`
- Changed `TradingService.buy(...)` and `TradingService.sell(...)` to load the user through the locking query
- Kept history reads and other non-trade reads unchanged

This fix intentionally favors explainable correctness over maximum same-user parallelism.

---

## After Behavior / Results

### Track A after
The focused Phase 9 observability test now reproduces and proves the new visibility:

| Measure | After result |
| --- | ---: |
| Active SSE subscribers | 6 |
| Quote publish cycles | 4 |
| Delivered quote events | 24 |
| Publish-recipient summary count | 4 |
| Publish-recipient total | 24 |
| Hot room subscriptions | 3 |
| Quiet room subscriptions | 1 |
| Chat sends recorded | 2 |

Prometheus scrape output now includes:
- `mockstock_quote_events_sent_total`
- `mockstock_quote_publish_recipients`
- `mockstock_quote_publish_latency_seconds_bucket`
- `mockstock_chat_send_latency_seconds_bucket`
- `mockstock_chat_room_subscriptions_active{roomId="..."}`

What improved:
- one quote publish cycle can now be explained as both count and cost
- hot-room concentration is visible instead of hidden inside aggregate session totals
- real-time paths now expose histogram-style timing data without requiring a dashboard first

### Track B after
The hardened concurrent-buy test result:

| Measure | After result |
| --- | ---: |
| Successful concurrent buys | 1 |
| Failed concurrent buys | 1 |
| Trade orders saved | 1 |
| Holding rows | 1 |
| Final holding quantity | 2 |
| Final cash balance | 300.00 |

Failure type:
- `BusinessValidationException`

What improved:
- the second request now waits for the first transaction and re-checks against committed cash
- impossible double-success behavior is blocked
- cash, holding quantity, and trade order history remain aligned

---

## Trade-Offs

- `mockstock.chat.room.subscriptions.active{roomId}` adds tag cardinality by room.
  That is acceptable in this local portfolio project because room count is bounded and the metric exists specifically to explain hot-room behavior.
- Pessimistic locking on the user row serializes same-user trade requests.
  That reduces concurrency for one user, but it is the simplest explainable integrity fix for this project stage.
- The concurrency fix locks the user row, not the entire trade graph.
  It solves the reproduced same-user buy race without turning the phase into a broader locking redesign.

---

## Local Verification

### Focused tests

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test --tests com.minsu.mockstocklive.phase7.Phase7MonitoringIntegrationTest --tests com.minsu.mockstocklive.phase8.Phase8LoadMetricsIntegrationTest --tests com.minsu.mockstocklive.phase9.Phase9ConcurrencyAndObservabilityIntegrationTest
```

### Real-time metrics checks

```powershell
Invoke-WebRequest http://localhost:8080/actuator/metrics/mockstock.quote.events.sent
Invoke-WebRequest http://localhost:8080/actuator/metrics/mockstock.quote.publish.recipients
Invoke-WebRequest http://localhost:8080/actuator/metrics/mockstock.quote.publish.latency
Invoke-WebRequest http://localhost:8080/actuator/metrics/mockstock.chat.send.latency
Invoke-WebRequest "http://localhost:8080/actuator/metrics/mockstock.chat.room.subscriptions.active?tag=roomId:1"
Invoke-WebRequest -Headers @{Accept='text/plain'} http://localhost:8080/actuator/prometheus
```

### Key Phase 9 verification output

```text
PHASE9_REALTIME afterSubscribers=6 beforeVisibleMetrics=2 afterDeliveredEvents=24 publishCycles=4 publishRecipientTotal=24 hotRoomSubscriptions=3 quietRoomSubscriptions=1 chatSendCount=2
PHASE9_CONCURRENT_BUY baselineSuccess=2 baselineOrders=2 baselineHoldingRows=1 baselineHoldingQuantity=2 baselineFinalCash=300.00 hardenedSuccess=1 hardenedFailures=1 hardenedOrders=1 hardenedHoldingRows=1 hardenedHoldingQuantity=2 hardenedFinalCash=300.00
```

---

## Remaining Limitations

- There is still no external SSE or WebSocket load generator in this phase.
- SSE visibility is still global; there is no per-symbol delivery metric.
- Trading still has no optimistic versioning or more granular holding-level locking strategy.
- The concurrency case focuses only on concurrent buy requests for the same user and stock.
  It does not yet cover concurrent sell or buy/sell interleaving cases.
- No dashboard JSON was added.
  The metrics are visible through actuator and Prometheus scrape output, which is enough for this portfolio stage.

---

## Interview-Ready Summary

This phase closed one observability gap and one correctness gap that were both easy to explain in interviews.

- Observability:
  I extended the monitoring foundation so the real-time side of the system exposes fan-out count, publish latency, chat send latency, and hot-room subscription skew.
- Correctness:
  I reproduced a same-user concurrent buy race where two requests could both succeed against the same starting balance, then fixed it with a pessimistic user-row lock.
- Result:
  The project now shows both measurable real-time behavior and an explicit before/after concurrency-hardening case without adding heavy infrastructure.
