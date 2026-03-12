# MockStock Live 포트폴리오 페이지 카피

## 제목

MockStock Live

부제:
실시간 모의 주식 거래 서비스를 통해 정합성, 조회 성능, 실시간 통신, 관측 가능성을 검증한 백엔드 포트폴리오

---

## 짧은 소개

MockStock Live는 외부 시세 API 없이 모의 주식 거래를 구현한 백엔드 중심 프로젝트입니다. 매수/매도, 보유 종목, 거래 이력, SSE 실시간 시세, WebSocket/STOMP 종목 채팅을 구현했고, 단순 기능 구현에 그치지 않고 조회 성능 병목과 동시성 정합성 문제를 실제로 재현하고 개선했습니다.

이 프로젝트의 핵심은 "무엇을 만들었는가"보다 "어떤 문제를 어떻게 측정하고 개선했는가"입니다.

---

## 기술 스택

- Backend: Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Spring Security
- Database: MySQL, Flyway
- Realtime: SSE, WebSocket / STOMP
- Observability: Actuator, Micrometer, Prometheus scrape
- Validation: k6, integration tests
- Frontend demo: React, Vite
- Deployment experience: Vercel, Railway

---

## 내가 구현한 것

- 회원가입 / 로그인
- 종목 목록 / 종목 상세 조회
- 매수 / 매도 / 거래 이력 / 보유 종목 조회
- SSE 기반 실시간 시세 스트리밍
- 종목별 WebSocket/STOMP 실시간 채팅
- Actuator 및 Prometheus-compatible 메트릭 노출
- React + Vite 기반 검증용 frontend demo

---

## 문제 해결과 개선

### 1. 채팅방 목록 조회 병목 개선

초기 채팅방 목록 조회는 서비스 레벨에서 방별로 stock, last message, membership을 반복 조회하는 구조였습니다. 이를 projection query 기반 단일 조회로 바꿔 SQL statement를 `128 -> 1`, 로컬 평균 응답 시간을 `42 ms -> < 1 ms`로 개선했습니다.

### 2. 거래 이력 조회 경로 개선

기존 거래 이력은 offset pagination 기반이었고, 깊은 페이지 접근에 적합하지 않았습니다. 비교를 위해 legacy endpoint는 유지한 채 cursor endpoint와 `(user_id, id)` 인덱스를 추가해 SQL statement를 `23 -> 2`, 로컬 평균 응답 시간을 `18 ms -> 13 ms`로 줄였습니다.

### 3. 동시 매수 정합성 하드닝

같은 사용자에 대한 동시 매수 요청을 재현했을 때 baseline에서는 두 요청이 모두 성공하며 불가능한 persisted state가 만들어졌습니다. 사용자 row에 대한 pessimistic locking을 적용해 결과를 `1건 성공 + 1건 validation failure`로 바꾸고 cash/holdings/trade history 정합성을 맞췄습니다.

### 4. 실시간 기능 관측 가능성 확보

실시간 시세와 채팅이 단순히 동작하는 수준에 머무르지 않도록 quote fan-out, publish latency, chat send latency, room-level subscription skew 메트릭을 추가했습니다. 이로 인해 SSE와 채팅의 runtime workload shape를 설명할 수 있게 됐습니다.

---

## 배포와 검증

이 프로젝트는 `2026-03-12`에 frontend는 Vercel, backend와 MySQL은 Railway로 분리 배포하는 흐름을 실제로 검증했습니다.

검증한 내용:

- Railway backend health/API 응답 확인
- Vercel frontend 접속 확인
- 배포된 frontend에서 trading, holdings, SSE, chat 동작 확인

중요:

- 이 프로젝트는 "배포 경험을 실제로 검증했다"는 사실은 말할 수 있습니다.
- 다만 비용 관리 때문에 backend/database가 항상 공개 live 상태라고 보장하지는 않습니다.

---

## 회고

이 프로젝트를 통해 기능 구현 자체보다 더 중요한 것은 문제를 재현 가능하게 만들고, 측정 가능한 형태로 설명하고, 가장 작은 안전한 수정으로 개선하는 과정이라는 점을 배웠습니다.

특히 이 프로젝트는 다음을 보여줍니다.

- 정합성이 중요한 쓰기 흐름을 다루는 방법
- read-heavy endpoint를 DTO/projection 중심으로 재설계하는 방법
- SSE와 WebSocket/STOMP를 목적에 맞게 나눠 쓰는 방법
- Observability를 기능과 함께 설계하는 방법
- 배포 과정의 build/runtime/CORS 문제를 실무적으로 정리하는 방법
