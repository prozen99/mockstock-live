# MockStockLive - Phase Handoff Document

## 문서 목적
이 문서는 MockStockLive 프로젝트를 새 채팅 세션, 새 작업 흐름, 또는 이후 포트폴리오 정리 단계에서
빠르게 이어가기 위한 핸드오프 문서다.

이 문서의 목적은 아래를 빠르게 전달하는 것이다.

- 현재 프로젝트가 어디까지 구현되었는가
- 어떤 기술 스택과 구조를 사용하고 있는가
- 어떤 기능이 검증되었는가
- 어떤 문제를 해결했고, 무엇이 아직 남아 있는가
- 다음 Phase에서 무엇을 해야 하는가

---

## 프로젝트 한 줄 요약
MockStockLive는 외부 주가 API 없이 가짜 종목 데이터를 사용해
모의 투자, 실시간 시세(SSE), 종목별 실시간 채팅(WebSocket/STOMP)을 제공하는
백엔드 중심 포트폴리오 프로젝트다.

핵심 목적은 단순 기능 구현이 아니라,
정합성, 조회 성능, 실시간 통신, 병목 재현, 개선, 문서화를
수치와 사례 중심으로 보여주는 것이다.

---

## 기술 스택
- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- MySQL
- Flyway
- SSE
- WebSocket / STOMP
- Gradle

---

## 로컬 개발 환경
- DB: MySQL local
- IDE: IntelliJ
- 테스트 도구:
    - Postman
    - Workbench
    - 간단한 HTML 테스트 페이지 (`chat-test.html`)
- OS: Windows
- 줄바꿈 이슈:
    - Codex patch 적용 실패 방지를 위해 문서/코드 줄바꿈은 LF 기준으로 관리하는 편이 안전함

---

## 현재까지 완료된 Phase

### Phase 1 - 프로젝트 구조 / 스켈레톤
완료 내용:
- 기본 패키지 구조 생성
- global config / exception / response / security skeleton 추가
- Flyway baseline 구조 추가
- 최소 공통 클래스 생성

대표 커밋:
- `c36c708` chore: add phase 1 package skeleton

---

### Phase 2 - auth / stock foundation
완료 내용:
- User / Stock 엔티티 및 repository 추가
- 회원가입 / 로그인 기본 API
- 종목 목록 / 종목 상세 조회 API
- users / stocks 테이블 생성 migration
- 로컬용 mock stock seed 데이터 추가

검증 완료:
- 회원가입 성공
- 로그인 성공
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{id}`

대표 커밋:
- `1138fb9` feat: add phase 2 auth and stock foundation

---

### Phase 3 - trading / holdings foundation
완료 내용:
- Holding / TradeOrder 엔티티 및 repository 추가
- 매수 / 매도 API
- 거래내역 조회 API
- 보유종목 조회 API
- holdings / trade_orders 테이블 생성 migration
- 잔고 부족 / 초과 매도 검증
- users.cash_balance, holdings, trade_orders를 같은 트랜잭션 안에서 처리

검증 완료:
- 매수 성공
- 매도 성공
- 거래내역 조회 성공
- 보유종목 조회 성공
- DB 반영 확인 완료

대표 커밋:
- `1dbc639` feat: add phase 3 trading and holdings foundation

---

### Phase 4 - mock quote generator / SSE
완료 내용:
- 가짜 시세 생성기 추가
- `stocks.current_price`, `stocks.price_change_rate` 주기적 갱신
- `GET /api/v1/quotes/stream`
- optional `?symbols=...` 필터링
- 기존 stock 조회 API도 갱신 가격 반영

검증 완료:
- `/api/v1/quotes/stream` 에서 `quote-snapshot` 및 `quote` 이벤트 확인
- 실시간 가격 변동 확인

추가 수정:
- SSE 끊김/타임아웃 이후 emitter 처리 과정에서 500 에러가 나던 문제 수정
- SSE 비동기 예외를 일반 JSON 에러 응답으로 감싸지 않도록 보완

대표 커밋:
- `e06b330` feat: add phase 4 mock quote streaming
- `27d5669` fix: stabilize phase 4 quote streaming

---

### Phase 5 - stock chat foundation
완료 내용:
- ChatRoom / ChatRoomMember / ChatMessage 엔티티 및 repository 추가
- 종목당 채팅방 1개 구조
- 채팅방 목록 조회 API
- 특정 방 메시지 조회 API
- 채팅방 join API
- WebSocket/STOMP 실시간 채팅
- 메시지 저장 후 브로드캐스트
- room.lastMessageId / lastMessageAt 업데이트

검증 완료:
- 채팅방 조회 성공
- join 성공
- 메시지 목록 조회 성공
- WebSocket 연결 성공
- SUBSCRIBE / SEND 성공
- 실시간 메시지 수신 성공
- DB `chat_messages` 저장 성공
- `chat_rooms.last_message_id`, `last_message_at` 업데이트 확인

대표 커밋:
- `1dc27e2` feat: add phase 5 stock chat foundation

---

### Phase 6 - 문제 재현 / 개선 / 문서화
선정한 문제:
1. 채팅방 목록 조회 비효율
2. 거래 내역 깊은 페이지 offset pagination 문제

완료 내용:
- chat room list 반복 조회 구조를 projection query 기반으로 개선
- trade history는 기존 offset 경로를 유지하면서 cursor 기반 경로 추가
- before / after 수치 측정
- 문서화 완료

대표 수치:
- Chat room list
    - Before: 128 SQL statements, 42 ms local average
    - After: 1 SQL statement, < 1 ms local average
- Trade history
    - Before: deep page(page=300, size=20) 23 SQL statements, 18 ms local average
    - After: cursor path 2 SQL statements, 13 ms local average

문서화:
- `docs/14-performance-lab.md`
- `docs/15-case-study-template.md`
- `docs/04-problem-scenarios.md` 갱신
- `docs/03-api-spec.md` 갱신

대표 커밋:
- `0758c26` perf: optimize phase 6 read paths

---

## 현재 구현된 주요 기능 목록

### Auth
- POST `/api/v1/auth/signup`
- POST `/api/v1/auth/login`

### Stock
- GET `/api/v1/stocks`
- GET `/api/v1/stocks/{stockId}`

### Trading / Portfolio
- POST `/api/v1/trades/buy`
- POST `/api/v1/trades/sell`
- GET `/api/v1/trades/history`
- GET `/api/v1/trades/history/cursor` (Phase 6 이후 추가된 개선 경로일 수 있음)
- GET `/api/v1/portfolio/holdings`

### Quotes
- GET `/api/v1/quotes/stream`
- optional: `?symbols=MSL001,MSL003`

### Chat
- GET `/api/v1/chat/rooms`
- GET `/api/v1/chat/rooms?userId={id}`
- GET `/api/v1/chat/rooms/{roomId}/messages?page={page}&size={size}`
- POST `/api/v1/chat/rooms/{roomId}/join`
- WebSocket endpoint: `/ws`
- STOMP subscribe: `/sub/chat/rooms/{roomId}`
- STOMP send: `/pub/chat/rooms/{roomId}`

---

## 현재 DB 주요 테이블
- users
- stocks
- holdings
- trade_orders
- chat_rooms
- chat_room_members
- chat_messages
- flyway_schema_history

---

## 현재 프로젝트에서 의도적으로 남겨둔 한계
이 프로젝트는 포트폴리오용 문제 해결 사례를 만들기 위해
일부 한계를 의도적으로 남겨두고 있다.

### 성능/구조상 남은 한계
- 채팅 메시지 이력은 offset pagination 기반
- 거래 내역은 legacy offset 경로가 아직 남아 있음
- unread count 최적화 미구현
- read status 로직 미완성
- SSE/WebSocket 연결 수 관측 부재
- 멀티 인스턴스 대응 없음
- Redis / Kafka / pub-sub 분산 구조 없음
- 대량 트래픽 기준 최적화 미적용

### 정합성/동시성 관련 남은 한계
- trading flow는 단일 요청 기준 정합성은 갖췄지만
  동시성 충돌 시나리오는 아직 심화 다루지 않음
- 강한 락/버전 기반 제어는 이후 단계 후보

---

## 현재 문서 세트
핵심 문서는 아래 기준으로 읽으면 된다.

- `docs/01-erd.md`
- `docs/03-api-spec.md`
- `docs/04-problem-scenarios.md`
- `docs/11-commit-rules.md`
- `docs/12-phase-rules.md`
- `docs/13-troubleshooting.md`
- `docs/14-performance-lab.md`
- `docs/15-case-study-template.md`

---

## 다음 Phase의 큰 방향
다음 단계(Phase 7 이후)는 “문제 해결 사례를 더 고도화하고,
관측/측정/정합성 증명”을 강화하는 쪽으로 가는 것이 맞다.

후보:
- 거래 정합성 / 동시성 시나리오 재현 및 개선
- 메시지 이력 pagination 개선
- unread count / room list 최적화
- 메트릭 / 모니터링 기초
- 성능 실험 결과를 README / 포트폴리오 본문으로 재구성

---

## 새 채팅에서 이어갈 때 핵심 지시
새 채팅에서는 아래를 전제로 이어가야 한다.

- 이 프로젝트는 기능 구현보다 “문제 재현 + 개선 + 문서화”가 더 중요하다
- 코드만 바꾸고 문서화하지 않으면 포트폴리오 가치가 떨어진다
- before / after 수치와 원인 분석이 꼭 남아야 한다
- local secret config 파일은 건드리지 않는다
- Phase 범위를 넘어가는 큰 기능 추가는 피한다

---

## 새 채팅에서 먼저 확인할 것
1. 현재 Phase 번호
2. 이번에 다룰 문제/병목 대상
3. `docs/14-performance-lab.md`에 기존 사례가 어떻게 정리되어 있는지
4. 새 작업도 동일한 문서화 수준을 유지할 것