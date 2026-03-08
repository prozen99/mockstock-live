# 실시간 설계

## 1. 시세는 왜 SSE인가
시세는 서버에서 클라이언트로 흘려보내는 단방향 이벤트다.
사용자가 시세를 서버로 보낼 일은 없다.
따라서 WebSocket보다 SSE가 더 단순하고 목적에 맞다.

## 2. 채팅은 왜 WebSocket인가
채팅은 사용자가 메시지를 보내고,
서버가 다시 방 참여자에게 브로드캐스트해야 한다.
즉 양방향 통신이 필요하므로 WebSocket/STOMP를 사용한다.

## 3. 역할 분리
- REST: 일반 조회/명령
- SSE: 시세/알림 스트림
- WebSocket: 채팅

## 4. 관리 포인트
- SSE active connections
- WebSocket active sessions
- broadcast count
- reconnect 전략
- payload 크기 최소화