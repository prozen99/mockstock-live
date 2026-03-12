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
  After adding new entities in this Boot 4 setup, verify that migration logs appear before Hibernate validation instead of assuming Flyway 1auto-configuration is active

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

### [2026-03-10] Phase 5 verification left temporary bootRun child processes alive

- phase: Phase 5
- situation:
  Started the application with `gradlew.bat bootRun --args=--server.port=8081` and `--server.port=8082` through PowerShell `Start-Process` for local HTTP verification, then tried to delete temporary log files
- error message:
  `The process cannot access the file ... because it is being used by another process.`
- cause:
  Stopping only the wrapper process was not enough. The spawned `MockStockLiveApplication` Java child processes kept running and still held the redirected log files open
- solution:
  Identified the remaining Java processes by command line, stopped the `MockStockLiveApplication --server.port=8081/8082` and matching `gradlew ... bootRun` processes, then removed the temporary log files
- prevention note:
  When using `Start-Process` for local Spring verification, stop both the wrapper and the child Java application process or use a single-process launch pattern that does not leave redirected log handles open

### [2026-03-10] Phase 4 시세가 안 바뀌는 것처럼 보였던 문제

- 단계: Phase 4
- 발생 상황:
  `GET /api/v1/stocks` 와 DB의 `stocks.current_price`, `price_change_rate`, `updated_at` 값을 여러 번 확인했는데 값이 바뀌지 않는 것처럼 보였음
- 증상 / 에러 메시지:
  별도 에러 메시지는 없었고, 시세 생성기와 SSE를 구현했는데 가격이 고정된 것처럼 보였음
- 원인:
  수정된 최신 서버가 아니라, 이전에 떠 있던 다른 애플리케이션 인스턴스가 8080 포트를 점유하고 있었음.  
  즉 실제 코드 문제가 아니라 **오래된 서버 인스턴스를 보고 있었던 환경 문제**였음
- 해결 방법:
  기존 서버 프로세스를 완전히 종료한 뒤 애플리케이션을 다시 실행하고,
  `GET /api/v1/stocks` 와 DB 쿼리로 재확인함
- 재발 방지 / 참고 사항:
  실시간 기능 검증 전에는 항상 **현재 실행 중인 서버가 최신 코드인지**,  
  그리고 **포트 충돌/중복 실행이 없는지** 먼저 확인할 것

---

### [2026-03-10] Phase 4 SSE 스트림(`/api/v1/quotes/stream`) 500 에러

- 단계: Phase 4
- 발생 상황:
  `GET /api/v1/quotes/stream` 테스트 중 500 에러 발생
- 증상 / 에러 메시지:
  공통 에러 응답으로 `INTERNAL_SERVER_ERROR` 가 반환되었고,
  서버 로그에는 SSE 비동기 처리 관련 예외가 출력됨
- 원인:
  SSE 클라이언트가 끊기거나 타임아웃된 뒤에도 스케줄러가 끊어진 emitter에 계속 이벤트를 보내려 했음.  
  그 과정에서 SSE 예외가 발생했고, `GlobalExceptionHandler` 가 이를 일반 JSON 에러 응답으로 감싸려고 하면서  
  `text/event-stream` 응답에 JSON을 쓰려 하여 추가 예외가 발생함
- 해결 방법:
  - `QuoteStreamService` 에서 끊어진 emitter는 `completeWithError()` 로 다시 건드리지 않고 **구독 목록에서 제거만** 하도록 수정
  - SSE payload 전송 시 `MediaType.APPLICATION_JSON` 을 명시
  - `GlobalExceptionHandler` 에서 SSE 비동기 예외(`AsyncRequestNotUsableException`)는 일반 JSON 응답으로 감싸지 않도록 처리
- 재발 방지 / 참고 사항:
  SSE/비동기 응답에서는 일반 REST 예외 응답 로직을 그대로 적용하면 안 됨.  
  끊어진 emitter는 안전하게 제거만 하고, SSE 예외는 별도 취급할 것

---

### [2026-03-10] SSE 테스트 시 GET 요청 Body를 넣어서 혼란이 생긴 문제

- 단계: Phase 4
- 발생 상황:
  Postman으로 `GET /api/v1/quotes/stream` 테스트 시 Body에 JSON 데이터를 넣고 요청함
- 증상 / 에러 메시지:
  요청은 정상처럼 보여도 테스트 방식이 잘못되어 원인 파악이 어려웠음
- 원인:
  SSE 엔드포인트는 `GET` 요청이며, 테스트 시 **Body 없이 호출**해야 하는데 일반 API처럼 Body를 넣고 테스트했음
- 해결 방법:
  - `GET /api/v1/quotes/stream` 은 Body 없이 호출
  - 필터링이 필요할 경우 `?symbols=MSL001,MSL003` 처럼 **query parameter** 로 전달
- 재발 방지 / 참고 사항:
  SSE 엔드포인트 테스트 시에는 Body를 넣지 말고,
  필요한 옵션은 query parameter로 전달할 것

---

### [2026-03-10] Phase 5 WebSocket 테스트 방법을 몰라 Postman UI에서 헤맨 문제

- 단계: Phase 5
- 발생 상황:
  STOMP/WebSocket 테스트를 하려고 했지만 Postman에서 WebSocket 요청 생성 위치를 찾지 못해 테스트가 지연됨
- 증상 / 에러 메시지:
  Postman에서는 일반 HTTP 요청 탭만 열리고, WebSocket 요청 생성 경로를 찾지 못해 테스트를 진행하지 못했음
- 원인:
  Postman의 현재 화면은 HTTP 요청용이었고,
  WebSocket/STOMP 테스트는 별도 요청 타입으로 만들어야 하는데 UI 구조를 처음 사용해서 찾지 못했음
- 해결 방법:
  Postman 대신 브라우저에서 바로 실행 가능한 **STOMP 테스트용 HTML 파일(`chat-test.html`)** 을 만들어 연결/구독/전송을 테스트함
- 재발 방지 / 참고 사항:
  WebSocket/STOMP를 처음 테스트할 때는 Postman보다
  간단한 HTML 테스트 페이지나 전용 클라이언트가 더 빠를 수 있음

---

### [2026-03-10] `chat-test.html` 실행 시 404가 발생한 문제

- 단계: Phase 5
- 발생 상황:
  `chat-test.html` 파일을 IDE 프리뷰 방식으로 열었더니 `localhost:63342/...` 주소에서 404 발생
- 증상 / 에러 메시지:
  브라우저에 `404 Not Found` 출력
- 원인:
  스프링 서버가 아니라 IDE/JetBrains 프리뷰 주소(`63342`)로 파일을 열어서 발생한 문제였음
- 해결 방법:
  - `chat-test.html` 을 브라우저에서 **파일 경로(`file:///...`)로 직접 열기**
  - 또는 `src/main/resources/static/chat-test.html` 로 옮겨서  
    `http://localhost:8080/chat-test.html` 로 열기
- 재발 방지 / 참고 사항:
  정적 테스트 페이지는
  - 파일로 직접 열거나
  - 스프링 `static` 경로로 서빙해서
    애플리케이션과 같은 포트에서 테스트하는 것이 가장 안정적임

---

### [2026-03-10] Phase 5 채팅 전송 전 join 선행이 필요한 점을 놓치기 쉬웠던 문제

- 단계: Phase 5
- 발생 상황:
  WebSocket으로 메시지를 보내기 전에 먼저 채팅방 참여(join)가 필요했음
- 증상 / 에러 메시지:
  join 없이 바로 전송하면 메시지 전송이 실패하거나 기대한 동작이 나오지 않을 가능성이 있었음
- 원인:
  현재 Phase 5 구현은 **join required before sending** 정책을 사용하고 있었음
- 해결 방법:
  먼저 `POST /api/v1/chat/rooms/{roomId}/join` 호출 후,
  그 다음 WebSocket CONNECT → SUBSCRIBE → SEND 순서로 테스트함
- 재발 방지 / 참고 사항:
  채팅 테스트 순서는 항상
  1. 방 조회
  2. join
  3. WebSocket 연결
  4. subscribe
  5. send
     순으로 진행할 것
---

---

### [2026-03-10] Phase 7 verification shell missing JAVA_HOME

- phase: Phase 7
- situation:
  Ran `./gradlew.bat test --tests com.minsu.mockstocklive.phase7.Phase7MonitoringIntegrationTest` from the current PowerShell session
- error message:
  `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.`
- cause:
  The shell session did not inherit a usable JDK path even though the machine already had JetBrains-managed JDKs under `C:\Users\admin\.jdks`
- solution:
  Set `JAVA_HOME=C:\Users\admin\.jdks\ms-21.0.10` and prepend `%JAVA_HOME%\bin` to `Path` before running Gradle verification
- prevention note:
  Configure the IDE terminal or user environment to point at the project JDK before build or test commands

### [2026-03-10] Phase 7 Prometheus scrape endpoint was not exposed by default

- phase: Phase 7
- situation:
  After adding Micrometer Prometheus support, the registry existed but local verification against `GET /actuator/prometheus` still failed
- error message:
  Initial verification produced `404 NOT_FOUND` for `/actuator/prometheus`, and requests with mismatched media expectations also surfaced `HttpMediaTypeNotAcceptableException: No acceptable representation`
- cause:
  In this Boot 4 local setup, adding the Prometheus registry dependency alone did not automatically publish a scrape endpoint under `/actuator/prometheus`
- solution:
  Added an explicit `PrometheusMeterRegistry` bean and a minimal actuator `prometheus` endpoint that returns `registry.scrape()`
- prevention note:
  When enabling Prometheus in this stack, verify both registry creation and actual HTTP scrape exposure instead of assuming the endpoint appears from dependency presence alone

### [2026-03-10] Phase 7 Prometheus endpoint verification needs text/plain

- phase: Phase 7
- situation:
  HTTP verification for the Prometheus scrape endpoint was attempted with default JSON-oriented client expectations
- error message:
  `HttpMediaTypeNotAcceptableException: No acceptable representation`
- cause:
  `/actuator/prometheus` is a text scrape endpoint, not a JSON endpoint
- solution:
  Request the endpoint with `Accept: text/plain`
- prevention note:
  Use the correct media type when verifying operational endpoints, especially scrape or streaming endpoints that do not return JSON

### [2026-03-11] Phase 8 local k6 install failed on the first winget package id

- phase: Phase 8
- situation:
  The Phase 8 load scripts were ready, but `k6` was not installed in the current environment
- error message:
  `No package found matching input criteria.`
- cause:
  The initial install attempt used the wrong package id. The correct Winget id is `GrafanaLabs.k6`, not `Grafana.k6`
- solution:
  Run `winget search k6`, install `GrafanaLabs.k6`, and invoke `C:\Program Files\k6\k6.exe` directly if the current shell has not refreshed `Path` yet
- prevention note:
  For local tooling installs, confirm the exact package id before scripting the command and do not assume the current shell will pick up PATH changes immediately

### [2026-03-11] Phase 8 packaged app hit a Prometheus endpoint bean collision

- phase: Phase 8
- situation:
  After adding Phase 8 load scripts and metrics, the packaged jar was started on `--server.port=8081` for runtime verification
- error message:
  `BeanDefinitionOverrideException: Invalid bean definition with name 'prometheusEndpoint'`
- cause:
  Spring Boot 4's Prometheus auto-configuration attempted to register its own `prometheusEndpoint` bean while this project already had an explicit local Prometheus registry plus custom actuator endpoint
- solution:
  Keep the explicit `PrometheusMeterRegistry` and custom actuator endpoint, and exclude `org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration` in `application.yml` so both tests and packaged runtime use the same single scrape implementation
- prevention note:
  When operational endpoints are customized in this Boot 4 setup, verify both the test context and the packaged runtime instead of assuming a monitoring configuration that works in one environment will behave the same in the other

### [2026-03-11] Phase 9 SSE cleanup test triggered AsyncContext reuse after client close

- phase: Combined observability and concurrency phase
- situation:
  The real-time observability integration test closed its HTTP SSE client streams and then triggered another explicit quote publish cycle
- error message:
  `IllegalStateException: A non-container (application) thread attempted to use the AsyncContext after an error had occurred`
- cause:
  The test forced another asynchronous SSE send after the client side had already closed the stream, so the application tried to write through an async context that was already in an error/closed state
- solution:
  Close the SSE client streams as cleanup only, remove the extra post-close publish call, and keep verification focused on metrics collected before connection teardown
- prevention note:
  For SSE verification, collect metrics while connections are still active and treat client close as the terminal cleanup step instead of triggering new async sends after teardown

### [2026-03-11] Phase 11 frontend verification shell was missing Node.js and npm

- phase: Phase 11
- situation:
  Started scaffolding the local React/Vite frontend demo and attempted to verify the frontend toolchain from the current PowerShell session
- error message:
  `node : The term 'node' is not recognized...`
  `npm : The term 'npm' is not recognized...`
- cause:
  Node.js was not installed or not present on the current shell `PATH`, so the frontend dependencies could not be installed or built from this environment
- solution:
  Install Node.js LTS, reopen the shell so `node` and `npm` are on `PATH`, then run `npm install` and `npm run dev` inside `frontend/`
- prevention note:
  Before starting frontend phases in this workspace, verify both `node -v` and `npm -v` in the active shell the same way the project already verifies `JAVA_HOME` for Gradle work

### [2026-03-11] Phase 11 npm install still failed until the Node install directory was added to PATH explicitly

- phase: Phase 11 local run verification
- situation:
  Node.js was installed and `node.exe` / `npm.cmd` were callable by absolute path, but the first frontend dependency install still failed
- error message:
  `npm error command C:\WINDOWS\system32\cmd.exe /d /s /c node install.js`
  `'node' is not recognized as an internal or external command`
- cause:
  The current shell did not have `C:\Program Files\nodejs` on `PATH`, so a child `cmd.exe` process launched during the `esbuild` install step could not resolve `node`
- solution:
  Prepend `C:\Program Files\nodejs` to `PATH` in the current shell before running `npm install`
- prevention note:
  After installing Node.js on Windows, verify not only direct `node.exe` execution but also that plain `node` resolves from the current shell before running npm-based setup steps

### [2026-03-12] Deployment-preparation audit found local-only profile assumptions that would block Railway startup

- phase: Deployment preparation for Vercel + Railway
- situation:
  Reviewed the current frontend and backend configuration before writing a manual deployment guide
- error message:
  No single runtime exception yet, but the deployment audit exposed three blockers:
  `spring.profiles.active: local` in the base config,
  mock stock seeding and quote generation restricted to the `local` profile,
  and HTTP CORS limited to `http://localhost:5173` while WebSocket allowed `*`
- cause:
  The project had been intentionally optimized for local verification first, so deployment concerns were not yet encoded in profile selection and cross-origin settings
- solution:
  Switched the base config to `spring.profiles.default=local`, added a `deploy` profile for Railway datasource/runtime settings, enabled mock stock seeding and quote generation in both `local` and `deploy`, and made HTTP/WebSocket allowed origins use the same environment-driven `APP_CORS_ALLOWED_ORIGIN_PATTERNS` setting
- prevention note:
  Before documenting any new manual deployment path, verify that startup profile selection, seed behavior, quote generation, and browser-origin rules still work outside the local-only profile
