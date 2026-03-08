# 부하 테스트 계획

## 시나리오 1. 거래 내역 조회
- 대상: GET /api/v1/trades/history
- 목적: offset vs keyset 비교

## 시나리오 2. 채팅방 목록 조회
- 대상: GET /api/v1/chat/rooms
- 목적: unread count / last message 포함 조회 성능 측정

## 시나리오 3. 보유 종목 조회
- 대상: GET /api/v1/portfolio/holdings
- 목적: projection vs 단순 entity 접근 방식 비교

## 시나리오 4. SSE 연결 유지
- 대상: GET /api/v1/quotes/stream
- 목적: 다수 연결 시 안정성 확인

## 시나리오 5. WebSocket 채팅 burst
- 대상: 종목별 채팅방
- 목적: 메시지 저장 및 브로드캐스트 처리량 확인

## 기록 항목
- 평균 응답 시간
- p95 latency
- 에러율
- DB connection usage
- CPU / memory