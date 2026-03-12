# MockStock Live 인터뷰 스토리 뱅크

## 프로젝트 개요

### Q. MockStock Live는 어떤 프로젝트인가요?

MockStock Live는 모의 주식 거래 서비스입니다. 매수/매도, 보유 종목, 거래 이력, SSE 실시간 시세, WebSocket/STOMP 종목 채팅을 구현했고, 단순 기능 구현보다 조회 성능 개선, 동시성 정합성, 관측 가능성까지 보여주는 백엔드 포트폴리오로 만들었습니다.

핵심은 "기능이 있다"보다 "문제를 재현하고 개선했다"는 점입니다.

---

## 왜 이 프로젝트를 만들었는가

### Q. 왜 외부 주식 API 없이 이런 프로젝트를 만들었나요?

외부 API를 붙이면 통합 자체는 보여줄 수 있지만, 제 목표였던 트랜잭션 정합성, 조회 경로 최적화, 실시간 fan-out, 메트릭 측정을 반복 가능한 조건에서 검증하기가 어려워집니다. 그래서 외부 의존성을 줄이고, 로컬과 테스트 환경에서 재현 가능한 조건을 만들어 백엔드 문제 해결에 집중했습니다.

트레이드오프는 명확합니다. 이 프로젝트는 외부 시세 API 연동이나 production market ingestion을 증명하지는 않습니다. 대신 백엔드 설계와 개선 과정을 더 선명하게 보여줍니다.

---

## 아키텍처 선택

### Q. 아키텍처를 왜 이렇게 나눴나요?

패키지는 `auth`, `stock`, `trading`, `portfolio`, `chat`, `monitoring`으로 나눴고, Controller는 얇게 유지하고 비즈니스 규칙은 Service에 두었습니다. JPA Entity를 API로 직접 노출하지 않고 DTO만 사용해서 HTTP contract와 persistence model을 분리했습니다.

이렇게 한 이유는 두 가지입니다. 첫째, 거래/보유 종목/이력 같은 핵심 규칙을 HTTP layer에 섞지 않기 위해서입니다. 둘째, read-heavy endpoint는 projection query나 cursor path처럼 별도 읽기 모델을 쓰기 쉽게 만들기 위해서입니다.

---

## 성능 최적화 사례

### Q. 가장 강한 성능 개선 사례를 하나 설명해 주세요.

가장 설명하기 좋은 사례는 채팅방 목록 조회입니다. 초기 구현은 채팅방 목록을 가져온 뒤 방별로 stock, last message, membership을 반복 조회하는 구조였고, 사실상 N+1 패턴이었습니다.

이걸 `ChatRoomRepository` projection query 하나로 바꿨고 API 응답 형태는 유지했습니다. 결과적으로 SQL statement가 `128 -> 1`, 로컬 평균 응답 시간이 `42 ms -> < 1 ms`로 줄었습니다.

이 사례의 포인트는 단순히 쿼리 튜닝이 아니라, 화면에 필요한 데이터에 맞춰 읽기 모델을 다시 설계했다는 점입니다.

### Q. cursor pagination은 왜 넣었고 결과는 어땠나요?

거래 이력은 append-only 성격이라 offset pagination이 깊은 페이지에서 불리합니다. 그래서 기존 offset endpoint는 baseline으로 유지하고, `beforeTradeId` 기반 cursor endpoint를 추가했습니다. `(user_id, id)` 인덱스도 함께 추가했습니다.

Phase 6 측정에서는 SQL statement가 `23 -> 2`, 로컬 평균 응답 시간이 `18 ms -> 13 ms`로 개선됐습니다. 다만 Phase 8의 moderate local load에서는 cursor가 p95에서 항상 더 빠르지는 않았습니다. 저는 이 부분을 오히려 강점으로 봅니다. cursor는 만능 최적화라기보다 history-style access에 더 맞는 계약이라는 점을 정직하게 설명할 수 있기 때문입니다.

---

## 동시성 / 정합성 사례

### Q. 이 프로젝트에서 가장 중요한 정합성 이슈는 무엇이었나요?

같은 사용자에 대한 동시 매수 race condition입니다. 두 요청이 같은 시작 잔액을 보고 동시에 통과하면, trade order는 두 건 저장되는데 final cash balance나 holding quantity는 한 번만 반영된 것처럼 보이는 불가능한 상태가 생길 수 있습니다.

저는 이 문제를 먼저 테스트로 재현했습니다. baseline에서는 동시 매수 두 건이 모두 성공했고, final cash는 `300.00`으로 남아 데이터가 어긋났습니다.

그 다음 `UserRepository.findByIdForUpdate(...)`에 `PESSIMISTIC_WRITE`를 적용하고, trade mutation에서 사용자 row를 locking read로 가져오도록 바꿨습니다. 이후 결과는 `1건 성공 + 1건 validation failure`로 바뀌었고 cash, holdings, trade history가 정렬됐습니다.

### Q. 왜 pessimistic locking을 선택했나요?

이 단계에서 가장 작은 안전한 수정이었기 때문입니다. optimistic locking도 가능하지만 retry/conflict-handling 설계를 추가로 설명해야 하고 범위가 커집니다. 이번 프로젝트에서는 same-user trade mutation에 대해 설명 가능한 correctness를 우선했습니다.

트레이드오프는 same-user parallelism이 줄어든다는 점입니다. 하지만 이 프로젝트 목적에는 그 선택이 더 적절했습니다.

---

## SSE / WebSocket 실시간 구현 사례

### Q. 왜 시세는 SSE이고 채팅은 WebSocket/STOMP인가요?

시세는 서버에서 클라이언트로 일방향 fan-out만 있으면 되기 때문에 SSE가 더 단순하고 브라우저 테스트도 쉽습니다. 반면 채팅은 양방향 메시징과 room subscription이 필요해서 WebSocket/STOMP가 더 맞습니다.

즉, "둘 다 실시간이라서 같은 기술을 쓴다"가 아니라, 트래픽 방향성과 상호작용 모델이 달라서 프로토콜을 다르게 골랐습니다.

### Q. 실시간 구현에서 기술적으로 무엇을 보여줄 수 있나요?

SSE 쪽은 subscription 관리, snapshot 전송, filtered symbol stream, publish cycle instrumentation까지 보여줄 수 있습니다. 채팅 쪽은 room join, `/ws` handshake, `/sub/chat/rooms/{roomId}` subscribe, `/pub/chat/rooms/{roomId}` send, message persistence, room metadata update까지 설명할 수 있습니다.

그리고 단순 구현을 넘어서 Phase 9에서 quote fan-out count, publish recipients, publish latency, room-level subscription skew를 추가해 실시간 workload shape를 관측 가능하게 만들었습니다.

---

## Observability / Monitoring 사례

### Q. 모니터링은 실제로 어디까지 했나요?

Actuator endpoint를 열고, Prometheus-compatible scrape endpoint를 직접 구성했고, trade/quote/chat/read-flow 메트릭을 추가했습니다. 예를 들어 trade request/validation failure, active SSE subscriptions, quote publish cycles, quote publish latency, delivered events, active WebSocket sessions, chat send latency, room-level subscription skew, read-flow request/latency를 수집합니다.

중요한 점은 "메트릭을 붙였다"가 아니라, 이후 k6 부하 검증과 concurrency test에서 이 메트릭들이 실제로 설명력을 가지게 만들었다는 점입니다.

### Q. k6 부하 검증에서 무엇을 확인했나요?

stock list, holdings, trade history를 burst read로 검증했습니다. 예를 들어 stock list run은 `137,574` requests, k6 avg `2.29 ms`, p95 `6.00 ms`였고, `hikaricp.connections.pending`은 `0.0`이어서 connection pool saturation이 아니라 application/query cost를 보고 있다는 점을 확인했습니다.

또 repository meter와 service-level timer를 같이 보면서 어디서 비용이 커지는지 설명할 수 있게 했습니다.

---

## 배포 / 트러블슈팅 사례

### Q. 배포 경험도 있나요?

네. `2026-03-12`에 frontend는 Vercel, backend와 MySQL은 Railway로 분리 배포하는 흐름을 실제로 검증했습니다. 다만 비용 관리를 위해 현재도 상시 live라고 보장하지는 않습니다. 정확한 표현은 "분리 배포 경험을 실제로 검증했다"입니다.

### Q. 배포에서 기억에 남는 문제는 무엇이었나요?

가장 실무적이었던 부분은 Railway의 서비스 식별과 build/runtime 설정이었습니다. duplicate service와 duplicate database가 있어서 어떤 서비스가 실제 active deployment인지부터 먼저 구분해야 했고, `There is no active deployment` 메시지도 잘못된 duplicate service에서 나온 것이었습니다.

또 Railway default build behavior는 이 repo에 충분히 안정적이지 않았고, 결국

- build: `chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon`
- start: `java -jar build/libs/mockstock-live-0.0.1-SNAPSHOT.jar`

로 명시했을 때 가장 예측 가능하게 동작했습니다.

추가로 `./gradlew: Permission denied`, backend root `/`가 JSON error를 반환하는데 실제 health endpoint는 살아 있는 상황, Vercel env에 `https://`를 포함한 full backend URL이 필요했던 점, Railway CORS에 deployed Vercel domain과 local origin을 함께 넣어야 했던 점이 있었습니다.

---

## 가장 어려웠던 문제와 해결

### Q. 가장 어려웠던 문제 하나만 꼽는다면요?

가장 설명 가치가 큰 문제는 동시 매수 정합성 문제입니다. 이유는 단순 버그 수정이 아니라, 단일 요청 기준으로는 멀쩡해 보이는 로직이 concurrent request에서만 깨지는 유형이기 때문입니다.

이 문제를 어렵게 만든 지점은 "어디가 느린가"가 아니라 "왜 persisted state가 논리적으로 불가능한가"를 증명해야 했다는 점입니다. 그래서 먼저 baseline을 재현하고, success/failure count, trade order count, holding row count, final cash balance까지 함께 검증했습니다.

그 다음 pessimistic locking으로 same-user trade mutation을 직렬화했고, 결과를 다시 측정해 `2건 성공`이 `1건 성공 + 1건 validation failure`로 바뀐 것을 확인했습니다.

이 경험을 통해 정합성 문제는 추상적인 이론보다, 재현 가능한 테스트와 persisted state 검증으로 보여줘야 한다는 점을 배웠습니다.

---

## 의도적으로 미룬 것들

### Q. 아직 안 한 것은 무엇이고 왜 미뤘나요?

- JWT/session 기반 authenticated user context
- chat message history cursor pagination
- unread/read-state 완성
- Redis/Kafka 기반 분산 실시간 아키텍처
- broader concurrency case
- tracing/log aggregation/dashboard JSON

이걸 한꺼번에 다 넣지 않은 이유는 범위를 넓히는 순간 각각의 문제 해결 스토리가 약해지기 때문입니다. 저는 이번 프로젝트에서 "기능 수"보다 "설명 가능한 개선 사례"를 우선했습니다.
