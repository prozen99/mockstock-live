# API Spec Draft

## Phase 3 Current Endpoints

### Auth
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

### Stocks
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{stockId}`

### Trading
- `POST /api/v1/trades/buy`
  Request body: `userId`, `stockId`, `quantity`
- `POST /api/v1/trades/sell`
  Request body: `userId`, `stockId`, `quantity`
- `GET /api/v1/trades/history?userId={id}&page={page}&size={size}`

### Portfolio
- `GET /api/v1/portfolio/holdings?userId={id}`

## Temporary Phase 3 Access Note

- Because JWT/session identity is still out of scope, trade and holdings APIs accept an explicit `userId`.
- This is temporary and should be replaced once authenticated user context exists in a later phase.

## Planned For Later Phases

The endpoints below stay out of Phase 3 scope and are not implemented yet.

### Auth
- `GET /api/v1/me`

### Stocks
- `GET /api/v1/stocks/{stockId}/ticks?beforeTickId={id}&size={size}`
- `GET /api/v1/quotes/stream?symbols=AAA,BBB`

### Portfolio
- `GET /api/v1/portfolio/snapshots?from=2026-03-01&to=2026-03-31`
- `GET /api/v1/rankings/profit-rate`

### Chat
- `GET /api/v1/chat/rooms`
- `GET /api/v1/chat/rooms/{roomId}/messages?beforeMessageId={id}&size={size}`
- `POST /api/v1/chat/rooms/{roomId}/read`

### Notifications
- `GET /api/v1/notifications`
- `POST /api/v1/notifications/{notificationId}/read`
- `GET /api/v1/notifications/stream`

### WebSocket
- `CONNECT /ws`
- `SUBSCRIBE /sub/chat/rooms/{roomId}`
- `SEND /pub/chat/rooms/{roomId}`
