# 커밋 규칙

## 1. 문서 목적
이 프로젝트는 포트폴리오 프로젝트이므로,
커밋도 단순 저장이 아니라 작업 흐름과 문제 해결 과정을 드러내야 한다.

따라서 커밋은 기능 단위가 아니라
“의미 있는 변경 단위”로 남긴다.

---

## 2. 기본 원칙

### 2-1. 한 커밋은 한 가지 주제만 다룬다
좋은 예:
- 인증 기본 구조 추가
- holdings 테이블 및 엔티티 추가
- 채팅방 목록 조회 쿼리 개선
- room list unread count 최적화

나쁜 예:
- 인증 + 채팅 + ERD 수정 + 쿼리 튜닝을 한 번에 커밋

---

### 2-2. 문서와 코드가 같이 바뀌면 같이 커밋한다
예:
- 테이블 구조 변경
- API 응답 구조 변경
- 성능 개선 결과 반영

---

### 2-3. 성능 개선 커밋은 반드시 전후 맥락이 있어야 한다
성능 관련 커밋은 아래 중 최소 하나를 포함해야 한다.
- before / after 수치
- 실행 계획 변화
- 쿼리 수 감소
- rows scanned 감소
- latency 개선

---

### 2-4. 깨진 상태는 커밋하지 않는다
원칙:
- 최소한 빌드가 돌아가야 한다
- 테스트가 있는 구간은 테스트 통과 후 커밋한다

---

## 3. 커밋 타입

### docs
문서 추가/수정

예:
- docs: add final erd and problem scenarios
- docs: update api spec for chat room read flow

### chore
환경 설정, 빌드 설정, 초기화, 의존성 조정

예:
- chore: initialize spring boot project
- chore: add local mysql config and gitignore

### feat
새 기능 추가

예:
- feat: add mock stock list and detail api
- feat: implement buy and sell trade flow
- feat: add websocket chat messaging

### fix
버그 수정

예:
- fix: prevent negative cash balance on concurrent buy
- fix: correct unread count calculation for empty rooms

### refactor
기능 변화 없이 구조 개선

예:
- refactor: split query repository from command repository
- refactor: move chat read logic into dedicated service

### perf
성능 개선

예:
- perf: optimize trade history with keyset pagination
- perf: reduce room list query count with projection query

### test
테스트 추가/수정

예:
- test: add concurrent buy integration test
- test: add room list query regression test

---

## 4. 커밋 메시지 형식

형식:
type: short summary

예:
- chore: initialize project docs and structure
- feat: add holdings summary api
- perf: optimize chat room list query
- test: add concurrent sell scenario test

---

## 5. 커밋 기준선

아래 경우에는 커밋해도 된다.

1. 의미 있는 파일 묶음이 완성됐을 때
2. 기능 한 단위가 동작할 때
3. 문서와 코드의 상태가 맞아떨어질 때
4. 성능 개선 한 사이클이 끝났을 때
5. 테스트 가능한 변경이 끝났을 때

---

## 6. 성능 개선 커밋 추가 규칙

성능 개선 작업은 가능하면 아래 흐름으로 커밋한다.

1. 느린 초기 구현
2. 문제 재현용 테스트/데이터 준비
3. 개선 코드 적용
4. 결과 문서 반영

예:
- feat: add initial offset-based trade history api
- test: add large trade history fixture
- perf: switch trade history to keyset pagination
- docs: record trade history pagination benchmark

이 흐름이 포트폴리오 스토리를 만들기 좋다.

---

## 7. Codex 작업 규칙
Codex에게 작업을 시킬 때는 아래를 같이 요구한다.

- 작업이 끝나면 변경 파일 요약
- 빌드/테스트 실행 여부 보고
- 가능하면 마지막에 커밋까지 수행
- 커밋 메시지는 이 문서 규칙을 따를 것

예시 지시문:
After completing the task, run relevant checks and create a git commit following docs/11-commit-rules.md.