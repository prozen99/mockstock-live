# Performance / Problem Reproduction Lab

## Scope
This Phase 6 record stays inside two narrow targets that already existed in the codebase:

1. `GET /api/v1/chat/rooms` repeated room-level lookups
2. `GET /api/v1/trades/history` deep-page offset pagination

The goal in this document is explicit:

- what problem was reproduced
- how it was measured
- what the root cause was
- what changed
- what improved after the change
- what still remains limited

All measurements below were captured on `2026-03-10` on the local profile with MySQL by
`com.minsu.mockstocklive.phase6.Phase6PerformanceLabIntegrationTest`.

---

## Case 1. Chat Room List Repeated Lookups

### Case title
Chat room list inefficiency from service-layer room preview assembly

### Why this case matters
`GET /api/v1/chat/rooms` is a read-heavy endpoint.
If each room requires extra lookups for stock info, last-message preview, and joined state, the cost grows with room count instead of staying bounded.

### Initial implementation
The original `ChatService.getRooms` flow loaded all rooms and then, per room:

1. lazy-loaded `room.stock`
2. fetched the last message preview by `last_message_id`
3. checked membership through `chat_room_members`

That made the endpoint behave like an N+1 query pattern.

### Reproduction setup
- Endpoint shape:
  `GET /api/v1/chat/rooms?userId={id}`
- Local dataset used in the Phase 6 test:
  - 43 rooms visible in the list
  - 40 additional Phase 6 rooms/messages seeded by the test
  - 1 user used for joined/not-joined evaluation
- Request pattern:
  - same list loaded repeatedly
  - average time measured across 5 runs

### Measurement method
- Hibernate `prepareStatementCount`
- local average elapsed time in milliseconds
- explicit baseline reproduction using the old service-layer assembly logic inside the integration test

### Before result
- SQL statements: `128`
- average elapsed time: `42 ms`

### Root cause
The endpoint did not use a list-specific read model.
Instead, it loaded `ChatRoom` entities and assembled list data in Java with repeated repository access for each row.

### Code/query change
- Added a projection query in `ChatRoomRepository`
- Joined `ChatRoom`, `Stock`, the last `ChatMessage`, and optional `ChatRoomMember` in one query
- Kept the response shape unchanged

### After result
- SQL statements: `1`
- average elapsed time: `< 1 ms` in the local 5-run average (`0 ms` after integer rounding)

### Remaining limitations
- unread count is still not included
- ordering is still the current room-order behavior
- measurement was captured on a local single-instance setup

### Interview-ready summary
The room list originally assembled stock info, last-message preview, and joined state per room, so query count grew with room count. I replaced that with a room-list projection query and kept the API shape unchanged. On the Phase 6 dataset, SQL statements dropped from `128` to `1` and local average latency dropped from `42 ms` to below `1 ms`.

---

## Case 2. Trade History Deep-Page Offset Pagination

### Case title
Trade history deep-page cost from offset pagination

### Why this case matters
Trade history is append-only and keeps growing.
Offset pagination is simple at first, but it becomes a poor fit for large histories and infinite-scroll style access.

### Initial implementation
The existing endpoint was:

- `GET /api/v1/trades/history?userId={id}&page={page}&size={size}`

It used:

- offset pagination through `PageRequest`
- a count query for total pages
- entity-to-DTO mapping after loading `TradeOrder`

### Reproduction setup
- Baseline endpoint:
  `GET /api/v1/trades/history?userId={id}&page=300&size=20`
- Improved endpoint:
  `GET /api/v1/trades/history/cursor?userId={id}&beforeTradeId={cursor}&size=20`
- Local dataset used in the Phase 6 test:
  - 12,000 trade rows for one user
  - 40 stocks rotated through the dataset so the response slice was not a single-stock cache case
- Request pattern:
  - compared the same logical slice through page `300`
  - average time measured across 5 runs

### Measurement method
- response equivalence check between the offset slice and the cursor slice
- Hibernate `prepareStatementCount`
- local average elapsed time in milliseconds
- MySQL `EXPLAIN` row estimate captured as an additional note

### Before result
- SQL statements: `23`
- average elapsed time: `18 ms`
- `EXPLAIN` rows estimate: `6001`

### Root cause
The baseline endpoint used offset pagination on an append-only history table and also paid for count-based page metadata plus entity loading.
That was acceptable as a first implementation, but not as a Phase 6 portfolio-worthy read strategy.

### Code/query change
- Added `GET /api/v1/trades/history/cursor`
- Added `TradeCursorHistoryResponse`
- Added a cursor query in `TradeOrderRepository` that returns `TradeHistoryItemResponse` directly
- Added `V5__add_trade_history_cursor_index.sql` with `trade_orders (user_id, id)`
- Kept the legacy page endpoint so before/after comparison stays explicit

### After result
- SQL statements: `2`
- average elapsed time: `13 ms`
- `EXPLAIN` rows estimate: `6001` on this local dataset

### Remaining limitations
- the legacy offset endpoint still exists for backward compatibility
- the cursor path assumes monotonically increasing trade ids for chronological traversal
- the local `EXPLAIN` rows estimate did not improve yet, so the strongest measured gain here is reduced query work rather than a dramatic plan change
- this phase does not add monitoring or load-test automation

### Interview-ready summary
The original trade-history API used page-number offset pagination, which is easy to build but a poor fit for a growing append-only history. I kept that endpoint as the baseline and added a cursor-based alternative with DTO projection plus an index on `(user_id, id)`. On the Phase 6 dataset, the same logical deep-history slice dropped from `23` SQL statements to `2`, and local average latency improved from `18 ms` to `13 ms`.
