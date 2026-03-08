# API 명세 초안

## 인증
- POST /api/v1/auth/signup
- POST /api/v1/auth/login
- GET /api/v1/me

## 종목
- GET /api/v1/stocks
- GET /api/v1/stocks/{stockId}
- GET /api/v1/stocks/{stockId}/ticks?beforeTickId={id}&size={size}
- GET /api/v1/quotes/stream?symbols=AAA,BBB

## 거래
- POST /api/v1/trades/buy
- POST /api/v1/trades/sell
- GET /api/v1/trades/history?beforeTradeId={id}&size={size}

## 포트폴리오
- GET /api/v1/portfolio/holdings
- GET /api/v1/portfolio/snapshots?from=2026-03-01&to=2026-03-31
- GET /api/v1/rankings/profit-rate

## 채팅
- GET /api/v1/chat/rooms
- GET /api/v1/chat/rooms/{roomId}/messages?beforeMessageId={id}&size={size}
- POST /api/v1/chat/rooms/{roomId}/read

## 알림
- GET /api/v1/notifications
- POST /api/v1/notifications/{notificationId}/read
- GET /api/v1/notifications/stream

## WebSocket
- CONNECT /ws
- SUBSCRIBE /sub/chat/rooms/{roomId}
- SEND /pub/chat/rooms/{roomId}