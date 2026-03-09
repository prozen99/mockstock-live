# Troubleshooting Log

## 문서 목적
이 문서는 MockStock Live 프로젝트를 진행하면서 발생한
로컬 개발 환경, DB, 애플리케이션 실행, 빌드, 인증, 배포 관련 문제를 기록한다.

목적은 단순 로그가 아니라,
문제 발생 → 원인 파악 → 해결 → 재발 방지 흐름을 남기는 것이다.

이 문서는 포트폴리오 관점에서 아래를 보여주기 위해 유지한다.

- 환경 문제를 스스로 해결한 과정
- 실행/설정 오류를 구조적으로 정리하는 습관
- 문제의 원인과 해결 방법을 문서화하는 능력
- 이후 동일 오류 재발 시 빠르게 대응하는 기준

---

## 기록 원칙
문제가 생기면 아래 형식으로 정리한다.

### 기록 항목
- 날짜
- 단계(예: Phase 1, Phase 2)
- 문제 제목
- 발생 상황
- 증상 / 에러 메시지
- 원인
- 해결 방법
- 재발 방지 / 참고 사항

---

## 템플릿

### [날짜] 문제 제목

- 단계:
- 발생 상황:
- 증상 / 에러 메시지:
- 원인:
- 해결 방법:
- 재발 방지 / 참고 사항:

---

## Troubleshooting Entries

### [2026-03-09] Git 명령어가 IntelliJ 터미널에서 인식되지 않음

- 단계: Phase 1 준비
- 발생 상황:
  Git을 설치한 뒤 IntelliJ PowerShell 터미널에서 `git init` 실행
- 증상 / 에러 메시지:
  `git : 'git' 용어가 cmdlet, 함수, 스크립트 파일 또는 실행할 수 있는 프로그램 이름으로 인식되지 않습니다.`
- 원인:
  Git 설치 후 IntelliJ가 이전 PATH 환경변수를 유지하고 있었음
- 해결 방법:
  IntelliJ를 완전히 종료 후 재실행하여 새 PATH 반영
- 재발 방지 / 참고 사항:
  개발 도구 설치 후 IDE 터미널에서 명령어가 안 먹으면 IDE 재시작 먼저 확인

### [2026-03-09] GitHub push 시 password authentication 실패

- 단계: Phase 1 준비
- 발생 상황:
  HTTPS remote로 `git push -u origin main` 실행
- 증상 / 에러 메시지:
  `Password authentication is not supported for Git operations.`
- 원인:
  GitHub는 Git 작업에서 일반 비밀번호 인증을 지원하지 않고 PAT 또는 브라우저 인증 필요
- 해결 방법:
  브라우저 인증 창을 통해 로그인하여 push 완료
- 재발 방지 / 참고 사항:
  이후 GitHub 인증은 비밀번호 대신 PAT 또는 브라우저 인증 사용

### [2026-03-09] Spring Boot 실행 시 8080 포트 충돌

- 단계: Phase 1 준비
- 발생 상황:
  애플리케이션 실행
- 증상 / 에러 메시지:
  `Web server failed to start. Port 8080 was already in use.`
- 원인:
  기존 실행 프로세스 또는 다른 서비스가 8080 사용 중
- 해결 방법:
  `netstat -ano | findstr :8080` 로 PID 확인 후 종료
- 재발 방지 / 참고 사항:
  포트 충돌 시 먼저 기존 프로세스 종료 후 재실행

### [2026-03-09] Gradle test 실행 시 JAVA_HOME 미설정

- 단계: Phase 1 scaffolding
- 발생 상황:
  Codex가 `./gradlew.bat test` 실행
- 증상 / 에러 메시지:
  `JAVA_HOME is not set and no 'java' command could be found in your PATH.`
- 원인:
  해당 셸 환경에서 JAVA_HOME / PATH가 설정되지 않음
- 해결 방법:
  JDK 21 경로를 셸 환경변수로 지정 후 테스트 실행
- 재발 방지 / 참고 사항:
  추후 시스템 환경 변수에 JAVA_HOME을 정식 등록하면 반복 해결 불필요

### [2026-03-09] application-local.yml 원격 저장소 업로드 여부 확인 필요

- 단계: Phase 1 준비
- 발생 상황:
  로컬 비밀 설정 파일이 GitHub에 올라갔는지 점검
- 증상 / 에러 메시지:
  직접적인 에러는 없었으나 비밀번호 노출 우려 존재
- 원인:
  로컬 전용 파일 관리 필요
- 해결 방법:
  `.gitignore`에 `src/main/resources/application-local.yml` 추가 및 원격 저장소 확인
- 재발 방지 / 참고 사항:
  로컬 비밀 설정 파일은 항상 gitignore 처리 후 push 전 원격 저장소 확인
### [2026-03-09] Phase 2 verification shell still missing JAVA_HOME

- phase: Phase 2
- situation:
  Running `./gradlew.bat test` from the current PowerShell session after implementing auth and stock foundations
- error message:
  `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.`
- cause:
  The shell session did not inherit a JDK path even though JetBrains had downloadable JDKs under `C:\Users\admin\.jdks`
- solution:
  Set `JAVA_HOME` explicitly to `C:\Users\admin\.jdks\ms-21.0.10` and prepend `%JAVA_HOME%\bin` to `Path` before running Gradle
- prevention note:
  Configure the IDE terminal or user environment so Gradle always sees the project JDK before verification commands run

### [2026-03-09] Flyway library present but schema migration did not run before JPA validation

- phase: Phase 2
- situation:
  The first context-load test after adding `users` and `stocks` entities failed even though the `V2__create_users_and_stocks.sql` migration existed
- error message:
  `Schema validation: missing table [stocks]`
- cause:
  Flyway was on the classpath through `org.flywaydb:flyway-mysql`, but this project did not have startup wiring that forced migration before Hibernate validation
- solution:
  Added an explicit `Flyway` bean in `src/main/java/com/minsu/mockstocklive/config/FlywayConfig.java` and made `entityManagerFactory` depend on `flyway`
- prevention note:
  After adding new entities in this Boot 4 setup, verify that migration logs appear before Hibernate validation instead of assuming Flyway auto-configuration is active

### [2026-03-10] Phase 4 bugfix verification blocked by existing app already using port 8080

- phase: Phase 4 bugfix
- situation:
  Attempted to start the current workspace build with `bootRun` to reproduce the quote persistence and SSE issues
- error message:
  `Web server failed to start. Port 8080 was already in use.`
- cause:
  An older Java process for MockStockLive was already bound to `8080`, so the new build could not start on the default port
- solution:
  Diagnosed the listener with `netstat -ano`, confirmed the existing Java process, and ran the current build on `--server.port=8081` for isolated verification
- prevention note:
  Before verifying runtime bugs locally, check whether another IDE-launched app instance is still bound to the default port or start the verification instance on a temporary alternate port
