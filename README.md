# MockStock Live

MockStock Live는 외부 API 없이 서버가 가상의 종목과 시세를 생성하고,
사용자가 모의 투자와 종목별 실시간 채팅을 할 수 있는 백엔드 중심 포트폴리오 프로젝트입니다.

이 프로젝트의 핵심은 단순 CRUD 구현이 아니라,
정합성, 실시간 네트워크 설계, DB 조회 최적화, 모니터링, 성능 개선을 수치로 증명하는 것입니다.

## 핵심 기능
- 회원가입 / 로그인
- 가상 종목 목록 / 상세 조회
- 모의 매수 / 매도
- 보유 종목 / 거래 내역 조회
- 실시간 시세 스트리밍
- 종목별 실시간 채팅
- 알림 / 읽음 처리
- 모니터링 / 부하 테스트

## 기술 스택
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- QueryDSL
- MySQL
- WebSocket / STOMP
- SSE
- Actuator
- Prometheus
- Grafana
- k6
- React + TypeScript

## 문서
- [프로젝트 개요](docs/00-project-overview.md)
- [ERD](docs/01-erd.md)
- [도메인 규칙](docs/02-domain-rules.md)
- [API 명세](docs/03-api-spec.md)
- [문제 시나리오](docs/04-problem-scenarios.md)
- [쿼리 튜닝 계획](docs/05-query-tuning-plan.md)
- [실시간 설계](docs/06-realtime-design.md)
- [모니터링 계획](docs/07-monitoring-plan.md)
- [부하 테스트 계획](docs/08-load-test-plan.md)
- [개발 로드맵](docs/09-development-roadmap.md)
- [포트폴리오 스토리](docs/10-portfolio-story.md)