# API Spec Draft

## Phase 2 Current Endpoints

### Auth
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

### Stocks
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{stockId}`

## Planned For Later Phases

The endpoints below stay out of Phase 2 scope and are not implemented yet.

### Auth
- `GET /api/v1/me`

### Stocks
- `GET /api/v1/stocks/{stockId}/ticks?beforeTickId={id}&size={size}`
- `GET /api/v1/quotes/stream?symbols=AAA,BBB`

### Trading
- `POST /api/v1/trades/buy`
- `POST /api/v1/trades/sell`
- `GET /api/v1/trades/history?beforeTradeId={id}&size={size}`

### Portfolio
- `GET /api/v1/portfolio/holdings`
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
