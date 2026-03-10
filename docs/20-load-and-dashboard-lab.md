# Load And Dashboard Lab

## Phase 8 Scope
Phase 8 stays local-friendly and buildable without Docker.

The goal is not to create production dashboards yet.
The goal is to prove that the existing monitoring foundation is useful during realistic request bursts and that the system can now tell a measurable load story.

All measurements below were captured on `2026-03-11` against the local profile on `http://localhost:8081`.

---

## Selected Scenarios

### 1. Stock list burst read
Why this scenario was chosen:
`GET /api/v1/stocks` is the simplest repeated read in the project and works as the baseline for how cheaply the application can serve a hot read path.

### 2. Holdings read burst
Why this scenario was chosen:
`GET /api/v1/portfolio/holdings` is still a read-heavy endpoint, but it includes user-scoped holdings plus response shaping from holding and stock data.

### 3. Trade history read under repeated access
Why this scenario was chosen:
Trade history was already a Phase 6 optimization case.
Phase 8 checks whether the monitoring foundation can distinguish the offset and cursor read paths under load instead of relying only on one-off before/after measurements.

---

## What Was Added

- `load-tests/k6/stocks-list-read.js`
  Local burst-read script for `GET /api/v1/stocks`
- `load-tests/k6/holdings-read.js`
  Local burst-read script for `GET /api/v1/portfolio/holdings`
- `load-tests/k6/trade-history-read.js`
  Local comparison script for `GET /api/v1/trades/history` and `GET /api/v1/trades/history/cursor`
- `load-tests/k6/lib/mockstock.js`
  Minimal setup helpers for signup, stock lookup, trading setup, and cursor lookup
- `monitoring/prometheus.local.yml`
  Optional local Prometheus scrape example for `GET /actuator/prometheus`
- `mockstock.read.requests{flow}`
  Read-flow counter used to confirm load actually hit the intended path
- `mockstock.read.latency{flow}`
  Read-flow timer used to compare service-side cost between selected read paths

---

## How To Run Locally

### 1. Start the application

If the shell does not already have Java configured:

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Then start the app on an alternate local port:

```powershell
.\gradlew.bat bootJar -x test
java -jar build/libs/mockstock-live-0.0.1-SNAPSHOT.jar --server.port=8081
```

### 2. Verify monitoring endpoints

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health
Invoke-WebRequest -Headers @{Accept='text/plain'} http://localhost:8081/actuator/prometheus
```

### 3. Run the k6 scenarios

```powershell
& 'C:\Program Files\k6\k6.exe' run --summary-export build/k6-stock-list-summary.json -e BASE_URL=http://localhost:8081 load-tests/k6/stocks-list-read.js
& 'C:\Program Files\k6\k6.exe' run --summary-export build/k6-holdings-summary.json -e BASE_URL=http://localhost:8081 load-tests/k6/holdings-read.js
& 'C:\Program Files\k6\k6.exe' run --summary-export build/k6-trade-offset-summary.json -e BASE_URL=http://localhost:8081 -e PAGINATION_MODE=offset -e TRADE_PAGE=15 -e TRADE_SIZE=20 -e TRADE_PAIRS=300 load-tests/k6/trade-history-read.js
& 'C:\Program Files\k6\k6.exe' run --summary-export build/k6-trade-cursor-summary.json -e BASE_URL=http://localhost:8081 -e PAGINATION_MODE=cursor -e TRADE_PAGE=15 -e TRADE_SIZE=20 -e TRADE_PAIRS=300 load-tests/k6/trade-history-read.js
```

### 4. Inspect the metrics after a run

```powershell
Invoke-RestMethod "http://localhost:8081/actuator/metrics/mockstock.read.requests?tag=flow:stock_list"
Invoke-RestMethod "http://localhost:8081/actuator/metrics/mockstock.read.latency?tag=flow:trade_history_offset"
Invoke-RestMethod "http://localhost:8081/actuator/metrics/mockstock.read.latency?tag=flow:trade_history_cursor"
Invoke-RestMethod "http://localhost:8081/actuator/metrics/spring.data.repository.invocations?tag=repository:TradeOrderRepository&tag=method:findTradeHistorySlice&tag=state:SUCCESS&tag=exception:None"
Invoke-RestMethod "http://localhost:8081/actuator/metrics/hikaricp.connections.pending"
```

If Prometheus is available locally, `monitoring/prometheus.local.yml` can be used as the starting scrape config.

---

## Metrics To Watch

- `mockstock.read.requests{flow}`
  Confirms which read path was exercised and at what scale.
- `mockstock.read.latency{flow}`
  Shows service-side time accumulation by flow.
- `spring.data.repository.invocations`
  Helps separate repository/query time from the rest of request handling.
- `hikaricp.connections.pending`
  Shows whether the load is queueing on the connection pool.
- `http_server_requests_seconds`
  Useful from the Prometheus scrape when comparing URI-level request timing to service-level flow timing.

---

## Observed Results

### Scenario summary

| Scenario | k6 shape | HTTP requests | k6 avg | k6 p95 | Flow metric count | Service avg from `mockstock.read.latency` |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Stock list read | 20 VUs, 20s | 137,574 | 2.29 ms | 6.00 ms | 137,577 | 0.29 ms |
| Holdings read | 15 VUs, 20s | 109,794 | 2.36 ms | 4.01 ms | 109,789 | 0.93 ms |
| Trade history offset | 12 VUs, 20s, page 15, size 20 | 69,717 | 3.21 ms | 5.00 ms | 69,100 | 1.92 ms |
| Trade history cursor | 12 VUs, 20s, page 15, size 20 | 52,427 | 4.45 ms | 5.62 ms | 51,840 | 3.20 ms |

Notes:

- All four runs completed with `0.00%` failed requests.
- `hikaricp.connections.pending` remained `0.0` during the measured runs, so this local workload did not saturate the connection pool.
- Flow metric counts are slightly different from total HTTP requests because setup calls and non-target requests are not all counted under the same read-flow tag.

### Repository visibility

The built-in repository meter made it possible to compare repository time with the higher-level flow timer:

| Repository method | Count | Total time | Derived avg |
| --- | ---: | ---: | ---: |
| `StockRepository.findAllByOrderBySymbolAsc` | 137,651 | 38.99 s | 0.28 ms |
| `HoldingRepository.findByUserIdOrderByIdAsc` | 109,789 | 31.05 s | 0.28 ms |
| `TradeOrderRepository.findByUserIdOrderByCreatedAtDescIdDesc` | 69,100 | 102.18 s | 1.48 ms |
| `TradeOrderRepository.findTradeHistorySlice` | 51,840 | 154.95 s | 2.99 ms |

---

## What Became Visible Under Load

### Stock list is the cheapest of the selected reads
The stock list held the highest request volume while keeping a low `p95`.
The repository invocation average stayed very close to the read-flow service average, which indicates this path is simple and cheap in the current local setup.

### Holdings stays fast, but more time is outside the repository call
The holdings repository call remained inexpensive, but the service-level read timer was noticeably higher than the repository average.
That suggests response assembly and object-to-DTO work matter more here than raw query time.

### Trade history is still the heaviest of the selected HTTP read paths
Both trade-history variants showed higher service-side averages than stock list and holdings.
That is expected because they return larger, history-oriented payloads and sit on top of more expensive repository work.

### Cursor pagination did not beat offset on this shallow local load shape
At `page=15` with `300` buy/sell pairs of setup data, the cursor path did not produce a lower local average or `p95` than the offset path.
That does not invalidate the Phase 6 pagination change.
It shows a more honest result: under moderate local depth, the cursor endpoint's value is better pagination behavior and deeper-history scalability headroom, not guaranteed lower latency on every shallow run.

### No immediate infrastructure saturation signal appeared
The load runs did not push Hikari into a pending state.
In other words, the tested local bottlenecks were still application and query path costs, not connection-pool exhaustion.

---

## Remaining Limitations

- This phase covers only local HTTP read bursts.
  It does not load-test SSE fan-out or WebSocket chat traffic yet.
- The read timers do not expose percentile histograms yet.
  `k6` provides the `p95`, but the application timer still reports count, total time, and max only.
- The trade-history comparison used a moderate local page depth, not a very deep history with thousands of rows per user.
- There is still no dashboard JSON in this phase.
  The Prometheus scrape config example was enough for local explanation without turning the phase into infrastructure work.
- The project still does not capture slow-query logs or database execution plans during load automatically.

---

## Phase 9 Extension Note

The next combined phase does not replace these HTTP read-load results.
It extends the observability story around real-time behavior:

- SSE quote publishing now exposes delivered-event count, recipient distribution per publish cycle, and publish latency histograms.
- Chat now exposes send latency and per-room active subscription gauges so hot-room concentration is visible.
- Those additions were verified through focused integration tests and actuator / Prometheus checks rather than a larger external load harness.

---

## Interview-Ready Summary

Phase 8 added local k6 validation on top of the Phase 7 monitoring foundation and focused on three portfolio-relevant read paths: stock list, holdings, and trade history.

The main outcome was not just request throughput numbers.
It was visibility:

- custom read counters proved exactly which flow was exercised
- read timers separated cheap reads from heavier history reads
- repository meters showed where query time dominated and where response shaping mattered more
- pool metrics confirmed the local runs were not waiting on DB connections

One useful nuance also became visible: cursor pagination is still the right long-term contract for trade history, but under a shallow local page depth it did not outperform the offset endpoint on `p95`.
That makes the Phase 8 story stronger, because the metrics support a precise claim instead of a blanket one.
