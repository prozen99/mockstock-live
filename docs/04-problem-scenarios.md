# Problem Scenarios

## 1. Purpose
This project is not meant to show only happy-path feature delivery.
It is a portfolio project that should also show how bottlenecks, correctness risks, and weak first implementations are discovered, measured, and improved.

This document records:

- which problems are intentionally worth reproducing
- why they matter in a real backend service
- how they can be measured
- which direction is appropriate for a minimal improvement

The core workflow is:

1. start with a simple implementation
2. reproduce the weakness under realistic data volume or request shape
3. measure the current behavior
4. apply the smallest meaningful improvement
5. compare before and after

---

## 2. Common Measurement Approach

### Measurement targets
- average response time
- p95 latency when available
- executed query count
- rows scanned or rows examined
- query plan differences
- correctness symptoms
- active real-time connection counts when relevant

### Recording format
Each case should be documented with:

- reproduced scenario
- initial implementation
- root cause
- measurement method
- before result
- code or query change
- after result
- remaining limitations

---

## 3. Scenario 1. Chat Room List Query Bottleneck

### 3-1. Problem
The chat room list is a read-heavy endpoint.
Users typically need the following fields together:

- stock name
- last message preview
- last activity time
- unread count or joined state

If the list is assembled with repeated per-room lookups, it becomes an N+1-style read path.

### 3-2. Initial implementation
A simple first implementation often looks like this:

1. load the room list
2. fetch the last message preview for each room separately
3. fetch membership or unread data for each room separately
4. fetch stock display data through lazy loading or additional queries

### 3-3. Why it becomes a problem
- query count grows with room count
- repeated lookup cost is paid on a frequently opened screen
- unread count logic can become expensive if implemented as repeated `count(*)`
- the endpoint remains simple in code but expensive in practice

### 3-4. Reproduction conditions
- one user joined to many rooms
- many rooms with last-message data
- repeated room list requests

### 3-5. Measurement method
- SQL statement count
- average response time
- query plan review if needed

### 3-6. Improvement direction
- room-list projection query
- use `chat_rooms.last_message_id` and `chat_rooms.last_message_at`
- avoid per-room repeated repository access

### 3-7. Expected improvement
- lower query count
- flatter latency growth as room count increases

### 3-8. Portfolio angle
This is a good example of a screen that looks simple but can still hide a meaningful backend read bottleneck.

---

## 4. Scenario 2. Trade History Offset Pagination Limitation

### 4-1. Problem
Trade history grows continuously over time.
Offset pagination is easy to build first, but it becomes a poor fit for deep-page access and infinite-scroll style usage.

### 4-2. Initial implementation
- `page`, `size`-based offset pagination
- latest trades loaded by page number

Examples:
- `page=0, size=30`
- `page=1000, size=30`

### 4-3. Why it becomes a problem
- the database pays skip cost for deep pages
- latency tends to worsen as the requested page gets deeper
- count-based page metadata can add avoidable work

### 4-4. Reproduction conditions
- one user with a large number of trade rows
- comparison across page depth such as `0`, `100`, `1000`, `5000`

### 4-5. Measurement method
- response time by page depth
- executed query count
- rows examined or scanned
- query plan comparison

### 4-6. Improvement direction
- keyset or cursor pagination using `beforeTradeId`
- supporting index such as `(user_id, id)`
- DTO projection for history rows

### 4-7. Expected improvement
- fewer queries
- more stable deep-history access
- pagination contract that fits append-only history better

### 4-8. Portfolio angle
This is a strong example of replacing a convenient first implementation with a more scalable read design.

---

## 5. Scenario 3. Chat Message History Offset Pagination Limitation

### 5-1. Problem
Message history can become very large for active stock rooms.
Offset pagination becomes increasingly inefficient for older pages.

### 5-2. Initial implementation
- `page`, `size`-based offset pagination
- latest messages loaded by descending id order

### 5-3. Why it becomes a problem
- deep-page lookups become slower
- active rooms can accumulate a large message volume quickly

### 5-4. Reproduction conditions
- one room with a very large message history
- comparison across shallow and deep pages

### 5-5. Measurement method
- response time by page depth
- rows scanned
- query plan comparison

### 5-6. Improvement direction
- keyset pagination with `beforeMessageId`
- index such as `(room_id, id desc)` or equivalent
- DTO projection for message reads

### 5-7. Expected improvement
- more stable historical message loading
- better fit for upward infinite scroll

### 5-8. Portfolio angle
This pairs naturally with trade history as a second cursor-pagination case.

---

## 6. Scenario 4. Holdings Query Over-Fetching

### 6-1. Problem
A holdings screen needs a compact set of display fields:

- stock name
- current price
- average buy price
- quantity
- evaluated amount
- profit or loss
- profit rate

If this is built by loading holdings and then repeatedly loading stocks or other entities, the endpoint can do more work than needed.

### 6-2. Initial implementation
1. load holdings
2. load stock data per holding
3. calculate response fields in the service layer

### 6-3. Why it becomes a problem
- possible N+1 behavior
- unnecessary entity loading
- response shape is smaller than the loaded object graph

### 6-4. Reproduction conditions
- one user with many holdings
- repeated holdings reads

### 6-5. Measurement method
- query count
- average response time
- selected column scope

### 6-6. Improvement direction
- holdings plus stock projection query
- select only required columns

### 6-7. Expected improvement
- fewer queries
- less object loading
- smaller read cost for the same screen

### 6-8. Portfolio angle
This demonstrates the difference between entity-oriented reads and screen-oriented read models.

---

## 7. Scenario 5. Trading Consistency Risk Under Repeated Requests

### 7-1. Problem
Buy and sell operations change cash balance, holdings, and trade history together.
If repeated requests hit the same user concurrently, correctness can break even when the normal single-request flow appears correct.

### 7-2. Initial implementation
- check current balance
- update balance
- update holdings
- save trade order

### 7-3. Why it becomes a problem
- concurrent buy requests can both pass the same balance check
- final balance, holdings, and order history can diverge
- correctness matters more than endpoint speed here

### 7-4. Reproduction conditions
- repeated concurrent buy requests for the same user
- starting cash that should allow only part of the total request volume

### 7-5. Measurement method
- final cash balance verification
- number of successful trades
- holdings quantity verification
- consistency between holdings and trade history

### 7-6. Improvement direction
- tighten transaction boundaries
- add simple locking or correctness hardening if needed
- validate with reproducible concurrent tests

### 7-7. Expected improvement
- no impossible cash state
- holdings and trade history stay aligned

### 7-8. Portfolio angle
This is a strong correctness case for backend interviews because it focuses on transactional integrity instead of only performance.

---

## 8. Scenario 6. SSE / WebSocket Visibility Gap

### 8-1. Problem
Real-time features can appear to work, but without visibility it is hard to understand connection count, publish behavior, or failure patterns.

### 8-2. Initial implementation
- SSE implemented
- WebSocket chat implemented
- little or no runtime visibility

### 8-3. Why it becomes a problem
- bottlenecks are hard to localize
- failure conditions are hard to explain
- real-time behavior is difficult to evaluate under load

### 8-4. Reproduction conditions
- multiple SSE connections
- multiple WebSocket sessions
- bursty chat or quote traffic

### 8-5. Measurement method
- active SSE connections
- active WebSocket sessions
- broadcast counts
- publish latency
- pool usage

### 8-6. Improvement direction
- metrics
- Prometheus
- Grafana

### 8-7. Expected improvement
- better operational visibility
- easier diagnosis under load

### 8-8. Portfolio angle
This shows that real-time features should be observable, not only implemented.

---

## 9. Scenario 7. Stock Detail Tick History Growth

### 9-1. Problem
Tick history can grow very quickly for an active stock.
Naive latest-tick queries may degrade as the table grows.

### 9-2. Initial implementation
- simple latest-tick queries by stock id

### 9-3. Why it becomes a problem
- sorting and range cost increase with data growth
- chart-style reads can become expensive

### 9-4. Reproduction conditions
- very large tick history per stock
- repeated latest-range reads

### 9-5. Measurement method
- response time
- query plan
- index usage
- rows scanned

### 9-6. Improvement direction
- index such as `(stock_id, tick_time desc)`
- query only the required range

### 9-7. Expected improvement
- more stable stock detail reads

---

## 10. Documentation Rule
Each completed case should eventually be written with:

- problem scenario name
- initial implementation
- reproduction setup
- measurement result
- root cause
- improvement
- after result
- remaining limitations
- next idea if relevant

This document feeds later portfolio documents such as `docs/10-portfolio-story.md` and the README.

---

## 11. Final Goal
The goal of this project is not simply to add many features.
The goal is to explain:

- which structure becomes a problem
- under what data or request shape the problem appears
- what changed
- what improved
- what limitations still remain

In other words, this document is the problem-and-improvement storyboard for the portfolio.

---

## 12. Phase 6 Selected Cases (2026-03-10)

### Case A. Chat room list repeated lookups
- Selected target:
  `GET /api/v1/chat/rooms`
- Why selected:
  the current implementation already matched the documented room-list inefficiency scenario and still assembled preview data with repeated room-level lookups.
- Phase 6 direction:
  reproduce the baseline query explosion, measure SQL statement count and local latency, then replace it with a single projection query without changing the API response.

### Case B. Trade history deep-page offset pagination
- Selected target:
  `GET /api/v1/trades/history`
- Why selected:
  the current implementation still used page-number offset pagination and was a strong portfolio example for large-history read design.
- Phase 6 direction:
  reproduce a deep-page request, measure the baseline, then add a cursor-based alternative with a minimal repository/query/index change.
