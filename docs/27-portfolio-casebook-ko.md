# MockStock Live 포트폴리오 케이스북

## 한 줄 요약

외부 시세 API 없이도 모의 주식 거래, 보유 종목, 거래 내역, SSE 실시간 시세, WebSocket/STOMP 종목 채팅을 구현하고, 조회 성능 개선과 동시성 정합성 문제를 실제로 재현·측정·개선한 백엔드 중심 포트폴리오 프로젝트입니다.

---

## 이 프로젝트를 왜 포트폴리오로 가져갈 가치가 있는가

이 프로젝트는 단순 CRUD 나열보다 "어떤 구조가 문제였고, 왜 문제였고, 무엇을 어떻게 바꿨으며, 실제로 무엇이 좋아졌는가"를 보여주는 데 초점을 맞췄습니다.

- 거래/보유 종목/거래 이력처럼 정합성이 중요한 쓰기 흐름이 있습니다.
- 채팅방 목록, 거래 내역처럼 읽기 비용이 커질 수 있는 조회 경로가 있습니다.
- SSE와 WebSocket/STOMP를 각각 다른 목적에 맞게 사용한 실시간 기능이 있습니다.
- Actuator, Micrometer, Prometheus scrape, k6를 통해 "보였다"가 아니라 "측정됐다"는 근거를 남겼습니다.
- Vercel + Railway 분리 배포를 실제로 검증했고, 배포 과정에서 겪은 문제와 해결 과정을 문서화했습니다.

---

## 프로젝트 포지셔닝

MockStock Live는 "기능이 많은 서비스"를 보여주기 위한 프로젝트라기보다, 백엔드 개발자로서 아래 역량을 보여주기 위한 프로젝트입니다.

- 트랜잭션 정합성을 우선하는 서비스 설계
- JPA/MySQL 기반 조회 경로 튜닝
- SSE / WebSocket-STOMP 프로토콜 선택과 구현
- 관측 가능성(Observability) 확보
- 측정 기반 개선 문서화

즉, "만들 수 있다"보다 "문제를 식별하고 설명 가능하게 개선할 수 있다"를 보여주는 포트폴리오입니다.

---

## 아키텍처 요약

### 기술 스택

- Java 21
- Spring Boot 4.0.3
- Spring MVC
- Spring Data JPA
- Spring Security
- MySQL
- Flyway
- Micrometer + Prometheus registry
- SSE
- WebSocket / STOMP
- Gradle
- React + Vite

### 구조 요약

- `auth`: 회원가입/로그인
- `stock`: 종목 조회, mock 시세 생성, SSE 시세 스트리밍
- `trading`: 매수/매도, 거래 이력, cursor 거래 이력
- `portfolio`: 보유 종목 조회
- `chat`: 채팅방 목록, 메시지 이력, 입장, STOMP 메시징
- `monitoring`: 커스텀 메트릭, Prometheus endpoint, room subscription metrics

### 설계 원칙

- Controller는 얇게 유지하고 비즈니스 규칙은 Service에 명시적으로 둠
- JPA Entity를 API 응답으로 직접 노출하지 않고 DTO만 사용
- 단순 CRUD와 read-heavy 경로를 같은 방식으로 처리하지 않음
- 문제가 생긴 읽기 경로는 projection query, cursor pagination 등으로 분리
- 스키마 변경은 Flyway migration으로 관리

---

## 구현된 기능 요약

### 인증

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`

### 종목/시세

- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{stockId}`
- `GET /api/v1/quotes/stream`
- `GET /api/v1/quotes/stream?symbols=...`

### 거래/포트폴리오

- `POST /api/v1/trades/buy`
- `POST /api/v1/trades/sell`
- `GET /api/v1/trades/history`
- `GET /api/v1/trades/history/cursor`
- `GET /api/v1/portfolio/holdings`

### 채팅

- `GET /api/v1/chat/rooms`
- `GET /api/v1/chat/rooms/{roomId}/messages`
- `POST /api/v1/chat/rooms/{roomId}/join`
- WebSocket endpoint `/ws`
- STOMP subscribe `/sub/chat/rooms/{roomId}`
- STOMP send `/pub/chat/rooms/{roomId}`

### 운영/관측

- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

---

## 가장 강한 엔지니어링 사례

## 1. 채팅방 목록 조회 성능 개선

문제:

- 채팅방 목록은 자주 열리는 read-heavy 화면인데, 초기에 서비스 레벨에서 방별로 stock, last message, membership 정보를 반복 조회하는 구조였습니다.
- 결과적으로 N+1 형태로 쿼리 수가 방 개수에 비례해 증가했습니다.

변경:

- `ChatRoomRepository`에 projection query를 추가해 채팅방 목록에 필요한 필드를 한 번에 조회하도록 바꿨습니다.
- API 응답 형태는 유지했습니다.

측정 결과:

- SQL statement 수: `128 -> 1`
- 로컬 평균 응답 시간: `42 ms -> < 1 ms`

의미:

- 단순히 "쿼리 최적화했다"가 아니라, 읽기 모델을 엔티티 중심이 아니라 화면 중심으로 재구성한 사례입니다.

## 2. 거래 이력 deep-page offset pagination 한계 개선

문제:

- 거래 이력은 append-only 성격이라 시간이 갈수록 커집니다.
- 초기 구현은 `page`, `size` 기반 offset pagination이었고, 깊은 페이지 접근에 적합하지 않았습니다.

변경:

- 기존 offset endpoint는 비교를 위해 유지하고,
- `beforeTradeId` 기반 cursor endpoint를 추가했습니다.
- `(user_id, id)` 인덱스를 추가하고 DTO projection 기반 조회 경로를 만들었습니다.

측정 결과:

- SQL statement 수: `23 -> 2`
- 로컬 평균 응답 시간: `18 ms -> 13 ms`

정직한 해석:

- Phase 8의 얕은 로컬 부하에서는 cursor가 offset보다 항상 더 빠르지는 않았습니다.
- 하지만 history형 조회 계약으로는 cursor가 더 적합하고, 깊은 이력 접근에 대한 구조적 여지를 만들었습니다.

## 3. 동시 매수 정합성 문제 재현 및 하드닝

문제:

- 같은 사용자에 대해 동시에 매수 요청이 들어오면, 두 요청이 같은 잔액을 기준으로 통과할 수 있었습니다.
- 이 경우 cash balance, holdings, trade history가 서로 맞지 않는 불가능한 상태가 생길 수 있었습니다.

재현 결과:

- 동일 사용자 동시 매수 baseline 결과:
  - 성공 `2`
  - 실패 `0`
  - trade orders `2`
  - final cash balance `300.00`

이 결과가 왜 문제인가:

- 주가가 `700.00`일 때 현금 `1000.00`으로는 한 번만 유효해야 합니다.
- 그런데 두 요청 모두 성공했고, persisted state는 일관되지 않았습니다.

변경:

- `UserRepository.findByIdForUpdate(...)`에 `PESSIMISTIC_WRITE`를 추가했습니다.
- 거래 mutation에서 사용자 row를 locking query로 읽도록 변경했습니다.

개선 결과:

- 성공 `1`
- 실패 `1`
- final cash / holdings / trade history 정렬 유지

의미:

- 성능 최적화가 아니라, 백엔드에서 더 중요한 "정합성" 문제를 재현 가능한 테스트와 함께 설명할 수 있는 사례입니다.

## 4. 실시간 기능에 대한 관측 가능성 확장

문제:

- SSE와 채팅은 동작하고 있었지만, publish cycle이 실제로 얼마나 많은 subscriber에게 전달됐는지, hot room이 어디인지, publish cost가 어땠는지 설명하기 어려웠습니다.

변경:

- `mockstock.quote.events.sent`
- `mockstock.quote.publish.recipients`
- `mockstock.quote.publish.latency`
- `mockstock.chat.send.latency`
- `mockstock.chat.room.subscriptions.active{roomId}`

를 추가했습니다.

측정 결과:

- active SSE subscribers: `6`
- quote publish cycles: `4`
- delivered quote events: `24`
- hot room subscriptions: `3`
- quiet room subscriptions: `1`
- chat sends recorded: `2`

의미:

- "실시간 기능이 있다" 수준이 아니라, 실시간 부하의 shape를 설명할 수 있는 상태로 바뀌었습니다.

---

## 측정 결과 요약

| 항목 | Before | After | 의미 |
| --- | ---: | ---: | --- |
| 채팅방 목록 조회 쿼리 수 | 128 | 1 | projection query로 N+1 제거 |
| 채팅방 목록 로컬 평균 응답 시간 | 42 ms | < 1 ms | 자주 열리는 화면의 조회 비용 대폭 감소 |
| 거래 이력 deep-page 쿼리 수 | 23 | 2 | offset 기반 baseline 대비 cursor 경로 단순화 |
| 거래 이력 로컬 평균 응답 시간 | 18 ms | 13 ms | deep-history read 경로 개선 |
| 동시 매수 성공 수 | 2 | 1 | 같은 사용자 거래 정합성 하드닝 |
| 동시 매수 실패 수 | 0 | 1 | validation failure로 불가능한 상태 차단 |

추가로 확인된 관측/부하 수치:

- Stock list burst read: `137,574` requests, k6 avg `2.29 ms`, p95 `6.00 ms`
- Holdings burst read: `109,794` requests, k6 avg `2.36 ms`, p95 `4.01 ms`
- Trade history offset burst read: `69,717` requests, k6 avg `3.21 ms`, p95 `5.00 ms`
- Trade history cursor burst read: `52,427` requests, k6 avg `4.45 ms`, p95 `5.62 ms`
- 모든 Phase 8 k6 run 실패율: `0.00%`
- `hikaricp.connections.pending`: 측정 구간에서 `0.0`

---

## 동시성 / 정합성 스토리

이 프로젝트에서 가장 인터뷰 가치가 큰 부분은 "동시성 문제를 실제로 재현했고, 결과가 왜 잘못됐는지 설명할 수 있으며, 가장 작은 안전한 수정으로 막았다"는 점입니다.

핵심 포인트:

- 거래는 cash balance, holdings, trade history를 함께 바꾸는 mutation입니다.
- 단일 요청 기준으로만 보면 정상처럼 보여도, 같은 사용자에 대한 concurrent request에서는 문제가 드러납니다.
- 저는 baseline을 먼저 재현했고, 그 다음 pessimistic locking으로 same-user trade mutation을 직렬화했습니다.
- 이 선택은 최대 병렬성보다 설명 가능한 정합성을 우선한 결정입니다.

트레이드오프:

- 같은 사용자에 대한 거래 요청 병렬성은 줄어듭니다.
- 하지만 이 단계에서는 retry/optimistic conflict 설계보다 명확한 integrity 보장이 더 중요했습니다.

---

## 관측 가능성 / 부하 검증 스토리

이 프로젝트는 모니터링을 "나중에 붙일 것"으로 남기지 않고, 기능과 함께 관측 가능한 상태로 만든 점이 강점입니다.

Phase 7:

- Actuator 기반 운영 surface 구성
- Prometheus-compatible endpoint 추가
- trade / quote / chat 메트릭 추가

Phase 8:

- read-heavy 경로에 대한 `mockstock.read.requests`, `mockstock.read.latency` 추가
- k6로 stock list, holdings, trade history를 로컬 burst read 검증

Phase 9:

- quote fan-out count
- publish recipients distribution
- publish latency
- room-level subscription skew
- chat send latency

즉, 이 프로젝트는 "기능 구현 -> 문제 재현 -> 측정 -> 개선 -> 다시 측정"의 루프를 모니터링과 부하 검증까지 확장해 보여줍니다.

---

## 배포 스토리

이 프로젝트는 `2026-03-12`에 아래 구조로 실제 배포 흐름을 검증했습니다.

- frontend: Vercel
- backend: Railway
- database: Railway MySQL

검증된 사실:

- Railway backend public URL 응답 확인
- Vercel frontend URL 응답 확인
- 배포된 frontend에서 signup/login/trading/holdings/SSE/chat 동작 확인

중요한 표현 원칙:

- 이 프로젝트는 "배포 경험을 실제로 검증했다"는 사실은 말할 수 있습니다.
- 하지만 Railway backend/database가 비용 관리 때문에 항상 켜져 있다고 보장할 수는 없습니다.
- 따라서 "현재도 항상 살아 있는 공개 demo"처럼 과장하지 않고, "분리 배포 경험을 검증했다"로 표현하는 것이 정직합니다.

배포 과정에서 얻은 실무 포인트:

- Railway에서는 중복 service/database가 생기면 실제 active deployment를 먼저 식별해야 합니다.
- default build detection보다 explicit build/start command가 더 안전했습니다.
- `./gradlew: Permission denied`는 `chmod +x ./gradlew`로 해결했습니다.
- backend root `/`는 health 신호가 아니므로 `/actuator/health`, `/api/v1/stocks`로 검증해야 했습니다.
- CORS는 배포된 Vercel domain과 local origin을 함께 고려해야 했습니다.

---

## 이 프로젝트를 통해 배운 점

- 단순 CRUD를 잘 만드는 것보다, read-heavy endpoint에 맞는 조회 모델을 따로 설계하는 것이 중요하다는 점
- 실시간 기능은 구현만으로 끝나지 않고, publish cost와 room skew까지 보여야 설명력이 생긴다는 점
- 트랜잭션 정합성 문제는 "이론적으로 위험하다"가 아니라 재현 가능한 테스트와 persisted state 검증으로 보여줘야 한다는 점
- cursor pagination은 항상 즉시 latency 승리를 주는 만능 답이 아니라, 데이터 접근 패턴에 맞는 계약이라는 점
- 배포도 기능만 띄우는 것이 아니라, build/runtime/CORS/service duplication 같은 운영성 문제를 함께 다뤄야 한다는 점

---

## 한계와 의도적으로 제외한 범위

- 외부 증권사/시세 API 연동 없음
- Redis, Kafka, 분산 pub-sub 없음
- unread/read-state 완성 없음
- chat message history cursor pagination 미구현
- JWT/session 기반 authenticated user context 미완성
- 대규모 production traffic 경험을 주장하지 않음
- always-on public demo를 보장하지 않음
- 대시보드 JSON, tracing, log aggregation 등 운영 인프라 확장은 범위 밖

이 범위는 "못 해서 빠진 것"이라기보다, 백엔드 문제 해결 사례를 선명하게 보여주기 위해 의도적으로 좁힌 범위에 가깝습니다.

---

## 이 프로젝트가 백엔드 개발자로서 나를 어떻게 증명하는가

이 프로젝트는 제가 다음을 할 수 있다는 증거입니다.

- Spring Boot + JPA + MySQL 기반으로 정합성이 중요한 도메인 흐름을 설계하고 구현할 수 있습니다.
- 기능을 만든 뒤 끝내지 않고, 병목과 정합성 문제를 재현하고 측정한 뒤 개선할 수 있습니다.
- SSE와 WebSocket/STOMP처럼 성격이 다른 실시간 프로토콜을 목적에 맞게 선택할 수 있습니다.
- Observability를 나중 일로 미루지 않고, 메트릭과 검증 흐름으로 시스템 상태를 설명할 수 있습니다.
- 배포 과정에서도 build, runtime, CORS, service identification 같은 실무 문제를 정리하고 기록할 수 있습니다.

한마디로 정리하면, 이 프로젝트는 "기능 구현자"가 아니라 "문제와 시스템을 설명 가능한 상태로 만드는 백엔드 개발자"라는 점을 보여주는 포트폴리오입니다.
