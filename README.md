# MockStock Live

MockStock Live는 외부 시세 API 없이 주식 거래를 시뮬레이션하는 백엔드 중심 포트폴리오 서비스입니다. 이 프로젝트는 단순한 기능 수보다 거래 정합성, 조회 경로 최적화, 실시간 통신, 그리고 측정 가능한 관측 가능성에 더 중점을 둡니다.

Phase 11에서는 기존 백엔드 흐름을 눈으로 쉽게 검증할 수 있도록 작은 React + Vite 프론트엔드 데모를 추가했습니다. 이 프로젝트는 여전히 백엔드 중심이며, 프론트엔드는 별도의 제품 빌드가 아니라 로컬에서 확인하기 위한 가벼운 데모 레이어입니다. 이 데모 화면에서는 활성 사용자 현금 잔액, 즉시 반영되는 거래 피드백, 보유 종목 손익, 실시간 SSE 시세, 종목별 채팅을 하나의 리뷰 친화적인 화면에서 확인할 수 있습니다.

## 프로젝트 개요

- 현금 잔액, 보유 종목, 거래 내역을 포함한 모의 주식 거래
- SSE 기반 실시간 시세 스트리밍
- WebSocket/STOMP 기반 종목별 실시간 채팅
- 명확한 전후 측정을 포함한 조회 중심 경로 최적화
- 모니터링 기반 구성, 로컬 부하 검증, 동시성 정합성 문서화

## 왜 이 프로젝트가 포트폴리오로 가치가 있는가

- 단순 CRUD 골격이 아니라 기능 구현과 문제 해결을 함께 보여줍니다.
- 비즈니스 핵심 흐름을 명확히 드러냅니다. 편의성보다 거래 정합성이 더 중요합니다.
- 막연한 성능 주장 대신 측정 가능한 개선 사례를 포함합니다.
- Actuator와 Prometheus 호환 메트릭을 통해 실시간 동작을 관찰할 수 있습니다.
- 트레이드오프와 남은 범위를 솔직하게 문서화합니다.

## 포트폴리오 핵심 포인트

- 채팅방 목록 조회 경로를 `128`개의 SQL 실행에서 `1`개로 개선했고, 로컬 평균 지연 시간은 `42 ms`에서 `< 1 ms`로 감소했습니다.
- 깊은 거래 내역 조회는 `23`개의 SQL 실행에서 `2`개로 개선했고, 로컬 평균 지연 시간은 `18 ms`에서 `13 ms`로 감소했습니다.
- 동일 사용자 동시 매수 경쟁 상황을 재현했고, `2`건의 잘못된 성공을 `1`건 성공 + `1`건 검증 실패로 보강하여 저장 상태와 결과가 일치하도록 만들었습니다.
- 관측 가능성은 이제 거래 검증 실패, SSE fan-out, 시세 발행 지연 시간, 채팅 전송 지연 시간, 방 단위 구독 편중까지 다룹니다.
- Vercel + Railway 분리 배포는 `2026-03-12` 기준으로 검증했지만, 이 저장소는 영구적으로 공개된 라이브 데모를 주장하지 않습니다.

## 핵심 기능

- `Auth`: 로컬 테스트용 회원가입 및 로그인
- `Stocks`: 로컬 시드 데이터 기반 종목 목록 및 종목 상세 조회
- `Trading`: 매수, 매도, 오프셋 기반 거래 내역, 커서 기반 거래 내역 API
- `Portfolio`: 사용자 보유 종목 요약 조회
- `Quotes`: 종목 필터링 옵션이 있는 SSE 시세 스트림
- `Chat`: 종목 채팅방 목록, 채팅 메시지 조회, 채팅방 입장, STOMP 채팅 메시징
- `Observability`: actuator 메트릭, Prometheus 수집 출력, 사용자 정의 런타임 메트릭
- `Validation`: 로컬 k6 조회 부하 검증 스크립트, 집중 동시성 통합 테스트
- `Frontend demo`: 로컬 시각 검증용 Phase 11 React/Vite UI, 현금 잔액과 즉시 반영되는 거래 상태 피드백 포함

## 기술 스택

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
- 로컬 부하 검증용 k6
- React
- Vite

## 아키텍처 요약

- 도메인 기준 계층형 패키지 구조: `auth`, `stock`, `trading`, `portfolio`, `chat`, `monitoring`
- 얇은 컨트롤러와 명시적인 서비스 계층 비즈니스 규칙
- DTO 전용 API 응답 사용, JPA 엔티티 직접 노출 금지
- 필요한 경우 단순한 커맨드 경로와 최적화된 조회 경로를 분리
- Flyway 기반 스키마 변경 관리 및 로컬 시드/초기화 지원
- 단방향 시세 전달에는 SSE, 양방향 채팅에는 WebSocket/STOMP 사용
- 거래, 조회 경로, 시세 fan-out, 채팅 활동에 대한 Actuator 및 Micrometer 계측

가벼운 다이어그램과 모듈 설명은 [docs/24-architecture-overview.md](docs/24-architecture-overview.md)에서 확인할 수 있습니다.

## 주요 문제 해결 사례

| 사례 | 처음에는 무엇이 문제였는가 | 적용한 최소 변경 | 측정 결과 |
| --- | --- | --- | --- |
| 채팅방 목록 조회 병목 | 서비스 계층에서 방을 조립하면서 반복 조회가 발생 | 방별 반복 접근을 하나의 프로젝션 쿼리로 교체 | `128`개 SQL → `1`개, 로컬 평균 `42 ms` → `< 1 ms` |
| 거래 내역 깊은 오프셋 페이징 | 거래 내역성 조회에서 오프셋 페이징 비용이 계속 큼 | 커서 API 추가 + `(user_id, id)` 인덱스 + DTO 프로젝션 적용 | `23`개 SQL → `2`개, 로컬 평균 `18 ms` → `13 ms` |
| 실시간 가시성 부족 | SSE/채팅은 있었지만 런타임 부하 형태를 설명하기 어려움 | 발행 fan-out, 지연 시간, 방 구독 메트릭 추가 | 시세 fan-out과 핫한 채팅방 집중도를 측정 가능하게 만듦 |
| 동시 매수 정합성 위험 | 동일 사용자 매수 2건이 같은 잔액 검증을 동시에 통과할 수 있었음 | 거래 변경 시 사용자 행에 비관적 락 적용 | 기존 `2`건 성공 → 보강 후 `1`건 성공 + `1`건 검증 실패, 저장 상태 일치 |

상세 측정 기록:

- [docs/14-performance-lab.md](docs/14-performance-lab.md)
- [docs/19-monitoring-foundation.md](docs/19-monitoring-foundation.md)
- [docs/20-load-and-dashboard-lab.md](docs/20-load-and-dashboard-lab.md)
- [docs/21-concurrency-and-observability-lab.md](docs/21-concurrency-and-observability-lab.md)

## 주요 API 및 실시간 기능

| 영역 | 기능 |
| --- | --- |
| Auth | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` |
| Stocks | `GET /api/v1/stocks`, `GET /api/v1/stocks/{stockId}` |
| Trading | `POST /api/v1/trades/buy`, `POST /api/v1/trades/sell`, `GET /api/v1/trades/history`, `GET /api/v1/trades/history/cursor` |
| Portfolio | `GET /api/v1/portfolio/holdings` |
| Quotes | `GET /api/v1/quotes/stream`, 선택 파라미터 `?symbols=MSL001,MSL003` |
| Chat HTTP | `GET /api/v1/chat/rooms`, `GET /api/v1/chat/rooms/{roomId}/messages`, `POST /api/v1/chat/rooms/{roomId}/join` |
| Chat WebSocket | `CONNECT /ws`, `SUBSCRIBE /sub/chat/rooms/{roomId}`, `SEND /pub/chat/rooms/{roomId}` |
| Operations | `GET /actuator/health`, `GET /actuator/metrics`, `GET /actuator/prometheus` |

전체 엔드포인트 목록은 [docs/03-api-spec.md](docs/03-api-spec.md)에 있습니다.

## 모니터링, 부하 검증 및 정합성 검증

- 모니터링 기반은 actuator 메트릭과 Prometheus 호환 수집 엔드포인트를 제공합니다.
- 사용자 정의 메트릭은 거래 요청 수, 거래 검증 실패 수, 활성 SSE 구독 수, 시세 fan-out, 시세 발행 지연 시간, 활성 WebSocket 세션 수, 채팅 전송 지연 시간, 방 단위 구독 편중을 추적합니다.
- 로컬 k6 스크립트로 종목 목록, 보유 종목, 거래 내역 조회를 검증합니다.
- 집중 동시성 테스트로 동일 사용자 동시 매수 경쟁 상황을 재현하고 보강합니다.

리뷰어 입장에서 가장 보기 좋은 문서는 다음과 같습니다.

- [docs/19-monitoring-foundation.md](docs/19-monitoring-foundation.md)
- [docs/20-load-and-dashboard-lab.md](docs/20-load-and-dashboard-lab.md)
- [docs/21-concurrency-and-observability-lab.md](docs/21-concurrency-and-observability-lab.md)

## 로컬 실행 방법

### 사전 준비

- Java 21
- 로컬에서 실행 중인 MySQL
- `src/main/resources/application-local.yml`에 로컬 시크릿 설정
- Phase 11 프론트엔드 데모를 실행하려면 Node.js LTS

중요:
`application-local.yml`은 로컬 전용 설정입니다. 시크릿을 커밋하지 말고, 로컬 환경 외부의 시크릿 관리 방식도 임의로 변경하지 마세요.

### 애플리케이션 실행

로컬 프로파일은 이미 `src/main/resources/application.yml`에서 기본 활성 프로파일로 설정되어 있습니다.

현재 셸에 Java 환경 변수가 설정되어 있지 않다면:


powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"


애플리케이션 실행:

.\gradlew.bat bootRun

만약 8080 포트가 이미 사용 중이라면:

.\gradlew.bat bootRun --args=--server.port=8081
빠른 검증
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/api/v1/stocks
Invoke-WebRequest -Headers @{Accept='text/plain'} http://localhost:8080/actuator/prometheus

채팅/STOMP 로컬 검증은 chat-test.html 또는 직접 사용하는 WebSocket 클라이언트를 이용하면 됩니다.

프론트엔드 데모 실행

frontend/ 디렉터리에서:

npm install
npm run dev

백엔드가 8080이 아닌 다른 포트에서 실행 중이라면 frontend/.env.local 파일을 만들고 다음을 설정합니다.

VITE_API_BASE_URL=http://localhost:8081   




리뷰어 진입 문서
포트폴리오 요약: docs/22-portfolio-summary.md
면접 대비: docs/23-interview-qna.md
아키텍처 개요: docs/24-architecture-overview.md
프론트엔드 데모: docs/25-frontend-demo.md
Vercel + Railway 배포 준비: docs/26-vercel-railway-deploy.md
API 개요: docs/03-api-spec.md
문제 시나리오: docs/04-problem-scenarios.md
성능 실험: docs/14-performance-lab.md
배포 준비

수동 배포 준비는 이제 분리 배포 구성을 기준으로 문서화되어 있습니다.

프론트엔드: Vercel
백엔드: Railway
MySQL: Railway

이 배포 흐름은 2026-03-12 기준으로 성공적으로 검증되었습니다.

실제 환경 변수, 실제로 동작했던 배포 순서, 검증 흐름, Railway/Vercel 배포 중 기록한 실제 트러블슈팅 노트는 docs/26-vercel-railway-deploy.md
에서 확인할 수 있습니다.

현재 의도적으로 제외한 범위
외부 시세 API 연동 없음
인프라 자동화 없음, 수동 Vercel + Railway 배포 준비만 포함
Redis, Kafka, 분산 실시간 fan-out 계층 없음
아직 인증된 사용자 컨텍스트 전파 없음, 일부 API는 여전히 명시적 userId 사용
핵심 동시성 문제를 측정하고 보강하기 전에는 모든 동시성 케이스를 다 해결하려 하지 않음

이 범위는 의도적인 선택입니다. 이 프로젝트는 포트폴리오 관점에서 절제된 백엔드 엔지니어링 판단, 측정 가능한 개선, 그리고 솔직한 트레이드오프를 보여주기 위해 설계되었습니다.
