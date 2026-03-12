# MockStock Live 이력서 불릿 모음

## 3줄 버전

- Spring Boot, JPA, MySQL 기반 모의 주식 거래 백엔드 프로젝트를 설계·구현하고, 매수/매도·보유 종목·거래 이력·SSE 시세·WebSocket/STOMP 채팅 기능을 구축했습니다.
- 채팅방 목록 조회를 projection query로 개선해 SQL statement를 `128 -> 1`, 로컬 평균 응답 시간을 `42 ms -> < 1 ms`로 줄였습니다.
- 같은 사용자 동시 매수 race condition을 재현한 뒤 pessimistic locking으로 하드닝해 `2건 동시 성공`을 `1건 성공 + 1건 validation failure`로 바꾸고 persisted state 정합성을 맞췄습니다.

---

## 5줄 버전

- Java 21, Spring Boot 4, Spring Data JPA, MySQL, Flyway 기반으로 모의 주식 거래 백엔드를 구현하고 DTO 중심 API 경계를 유지했습니다.
- 매수/매도, 보유 종목, 거래 이력, SSE 실시간 시세, 종목별 WebSocket/STOMP 채팅을 하나의 서비스 흐름으로 통합했습니다.
- 채팅방 목록 조회 경로를 서비스 레벨 반복 조회에서 projection query로 전환해 SQL statement를 `128 -> 1`, 로컬 평균 응답 시간을 `42 ms -> < 1 ms`로 개선했습니다.
- 거래 이력 deep-page offset pagination 한계를 개선하기 위해 cursor endpoint와 `(user_id, id)` 인덱스를 추가해 SQL statement를 `23 -> 2`, 로컬 평균 응답 시간을 `18 ms -> 13 ms`로 줄였습니다.
- Actuator, Micrometer, Prometheus scrape, k6를 활용해 read-heavy 경로와 실시간 경로를 측정 가능하게 만들고, Vercel + Railway 분리 배포 흐름을 `2026-03-12`에 실제 검증했습니다.

---

## 8줄 버전

- Spring Boot 4.0.3, Spring Data JPA, MySQL, Flyway 기반으로 백엔드 중심 포트폴리오 서비스 `MockStock Live`를 설계·구현했습니다.
- 외부 시세 API 없이 mock stock data와 자체 quote generator를 사용해 종목 조회, 매수/매도, 보유 종목, 거래 이력, 실시간 시세, 종목 채팅 기능을 구성했습니다.
- Quote streaming은 SSE, 채팅은 WebSocket/STOMP로 분리 설계해 one-way fan-out과 bidirectional messaging을 각각 목적에 맞는 프로토콜로 구현했습니다.
- 채팅방 목록 조회 병목을 재현한 뒤 projection query로 개선해 SQL statement를 `128 -> 1`, 로컬 평균 응답 시간을 `42 ms -> < 1 ms`로 줄였습니다.
- 거래 이력의 deep-page offset pagination 한계를 확인하고 cursor endpoint와 `(user_id, id)` 인덱스를 추가해 SQL statement를 `23 -> 2`, 로컬 평균 응답 시간을 `18 ms -> 13 ms`로 개선했습니다.
- 같은 사용자 동시 매수 요청 race condition을 테스트로 재현하고 `PESSIMISTIC_WRITE` 기반 locking을 적용해 baseline `2건 성공`을 `1건 성공 + 1건 validation failure`로 바꾸며 cash/holdings/trade history 정합성을 맞췄습니다.
- Actuator, Micrometer, Prometheus-compatible endpoint, k6를 도입해 trade validation failure, SSE fan-out, publish latency, room-level subscription skew, read-flow latency를 측정 가능하게 만들었습니다.
- Vercel(frontend) + Railway(backend/MySQL) 분리 배포를 실제로 검증했으며, Railway build/start command, CORS, duplicate service 식별, `gradlew` 실행 권한 문제를 포함한 배포 트러블슈팅을 문서화했습니다.
