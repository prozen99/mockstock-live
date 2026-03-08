# 모니터링 계획

## 목표
단순히 기능이 동작하는 수준이 아니라,
부하 상황에서 어떤 지점이 병목인지 식별할 수 있어야 한다.

## 기본 지표
- HTTP 요청 수
- API 응답 시간
- 에러율
- JVM heap
- GC
- HikariCP active connections

## 커스텀 메트릭
- mockstock.quote.broadcast.count
- mockstock.websocket.sessions.active
- mockstock.sse.connections.active
- mockstock.trade.buy.success
- mockstock.trade.buy.fail
- mockstock.chat.room-list.latency

## 대시보드 초안
1. API overview
2. DB / connection pool
3. SSE / WebSocket
4. 거래 성공 / 실패
5. 채팅방 목록 API latency