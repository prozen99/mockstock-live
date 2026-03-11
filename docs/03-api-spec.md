# API Spec

## Current Implemented Endpoints (Phase 9)

### Auth
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

### Stocks
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{stockId}`

### Quotes
- `GET /api/v1/quotes/stream`
- `GET /api/v1/quotes/stream?symbols=MSL001,MSL003`

### Trading
- `POST /api/v1/trades/buy`
  Request body: `userId`, `stockId`, `quantity`
- `POST /api/v1/trades/sell`
  Request body: `userId`, `stockId`, `quantity`
- `GET /api/v1/trades/history?userId={id}&page={page}&size={size}`
- `GET /api/v1/trades/history/cursor?userId={id}&beforeTradeId={id?}&size={size}`
  Cursor response metadata: `requestedBeforeTradeId`, `size`, `hasNext`, `nextBeforeTradeId`

### Portfolio
- `GET /api/v1/portfolio/holdings?userId={id}`

### Chat
- `GET /api/v1/chat/rooms`
- `GET /api/v1/chat/rooms?userId={id}`
- `GET /api/v1/chat/rooms/{roomId}/messages?page={page}&size={size}`
- `POST /api/v1/chat/rooms/{roomId}/join`
  Request body: `userId`

### WebSocket
- `CONNECT /ws`
- `SUBSCRIBE /sub/chat/rooms/{roomId}`
- `SEND /pub/chat/rooms/{roomId}`
  Request body: `userId`, `content`

### Operations
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/metrics/{metricName}`
- `GET /actuator/prometheus`
  Prometheus scrape endpoint. Request it with `Accept: text/plain`.

## Phase 6-9 Notes

- `GET /api/v1/chat/rooms` keeps the same response shape, but the implementation now uses a room-list projection query instead of service-layer repeated lookups.
- `GET /api/v1/trades/history` remains available as the legacy offset-based baseline for comparison and backward compatibility.
- `GET /api/v1/trades/history/cursor` is the Phase 6 cursor-based alternative for large trade histories.
- Phase 7 adds actuator-based operational visibility and Prometheus-compatible metrics exposure for trading, quote streaming, and chat activity.
- Custom Phase 7 metrics are available through `/actuator/metrics/{metricName}` and `/actuator/prometheus`.
- Phase 8 adds read-flow instrumentation used by the local k6 load-validation lab.
- Phase 9 extends the real-time metrics with quote fan-out, quote publish latency, chat send latency, and room-level subscription visibility.

## Current Temporary Access Note

- Because JWT/session identity is still out of scope, trade, holdings, and chat APIs accept an explicit `userId` where needed.
- This is temporary and should be replaced once authenticated user context exists in a later phase.
- Quote streaming is anonymous for now so it stays easy to test from a browser or Postman.
- Chat room reads are open for local testing, but joining and sending messages use the temporary explicit `userId` flow.

## Planned For Later Phases

The endpoints below stay out of the current Phase 10 scope and are not implemented yet.

### Auth
- `GET /api/v1/me`

### Stocks
- `GET /api/v1/stocks/{stockId}/ticks?beforeTickId={id}&size={size}`

### Portfolio
- `GET /api/v1/portfolio/snapshots?from=2026-03-01&to=2026-03-31`
- `GET /api/v1/rankings/profit-rate`

### Chat
- `GET /api/v1/chat/rooms/{roomId}/messages?beforeMessageId={id}&size={size}`
- `POST /api/v1/chat/rooms/{roomId}/read`

### Notifications
- `GET /api/v1/notifications`
- `POST /api/v1/notifications/{notificationId}/read`
- `GET /api/v1/notifications/stream`
