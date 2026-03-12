# MockStock Live 스크린샷 체크리스트

이 문서는 이미지를 생성하기 위한 문서가 아니라, 나중에 포트폴리오용 스크린샷을 직접 캡처할 때 무엇을 찍어야 하고 각 화면이 무엇을 증명해야 하는지 정리한 체크리스트입니다.

핵심 원칙:

- UI 예쁨보다 백엔드 기능과 검증 포인트가 드러나야 합니다.
- 한 장의 화면이 "무엇이 구현됐는지"보다 "무엇이 확인됐는지"를 증명해야 합니다.
- 가능하면 URL, 상태 텍스트, 응답 결과, 메트릭 이름, 문서 제목이 같이 보이게 캡처합니다.

---

## 1. 종목 목록 / 종목 상세

### 캡처 대상

- frontend의 stock list
- selected stock detail panel

### 함께 보이면 좋은 것

- selected 상태
- 현재가 / 등락률
- updated 시각

### 이 스크린샷이 증명해야 하는 것

- 종목 목록과 상세 조회 기능이 구현되어 있음
- mock quote가 현재 화면에서 사용되고 있음
- frontend demo가 백엔드 종목 조회 API와 연결되어 있음

---

## 2. 매수 / 매도 실행

### 캡처 대상

- buy 또는 sell 직후의 화면

### 함께 보이면 좋은 것

- selected user
- quantity 입력값
- trade feedback banner
- remaining cash balance

### 이 스크린샷이 증명해야 하는 것

- trade API가 실제로 동작함
- 응답 결과가 즉시 UI에 반영됨
- cash balance가 trade response와 연동되어 갱신됨

---

## 3. 보유 종목 / 거래 이력

### 캡처 대상

- holdings table
- trade history table

### 함께 보이면 좋은 것

- 종목명 / 수량 / 평균 매수가 / 평가금액 / 손익
- 거래 타입(BUY/SELL)
- 페이지 이동 UI 또는 trade history section 제목

### 이 스크린샷이 증명해야 하는 것

- 보유 종목 조회와 거래 이력 조회가 구현되어 있음
- trade 이후 portfolio/history read path가 실제로 연결되어 있음
- 단순 거래 API만 있는 것이 아니라, read model도 확인 가능함

---

## 4. SSE 실시간 업데이트

### 캡처 대상

- quote status가 connected 상태인 화면
- 이전/이후 가격이 달라진 상태가 보이는 화면

### 함께 보이면 좋은 것

- `Quotes connected` 또는 유사 상태 텍스트
- `publishedAt` 혹은 갱신 시각
- 동일 종목 가격이 갱신된 흔적

### 이 스크린샷이 증명해야 하는 것

- SSE 연결이 실제로 수립됨
- 실시간 시세가 polling 없이 갱신됨
- frontend demo가 quote stream과 연결되어 있음

---

## 5. 채팅 send / receive

### 캡처 대상

- room list
- join 이후 상태
- send 후 메시지가 추가된 화면

### 함께 보이면 좋은 것

- connected 상태
- joined 상태
- room name / stock symbol
- 메시지 sender / timestamp

### 이 스크린샷이 증명해야 하는 것

- WebSocket/STOMP 연결이 실제로 동작함
- room join 이후 메시지 송신이 가능함
- 채팅 메시지가 persistence 및 room update 흐름과 연결되어 있음

---

## 6. 모니터링 / 메트릭 근거

### 캡처 대상

- `/actuator/metrics`
- `/actuator/metrics/mockstock.quote.publish.latency`
- `/actuator/metrics/mockstock.chat.room.subscriptions.active?...`
- `/actuator/prometheus`

### 함께 보이면 좋은 것

- metric name
- tag
- count / total / max
- prometheus text output 일부

### 이 스크린샷이 증명해야 하는 것

- 관측 가능성이 실제로 구현되어 있음
- 실시간 fan-out, room skew, read flow 같은 설명 포인트가 숫자로 노출됨
- "메트릭을 붙였다"가 아니라 실제 조회 가능한 운영 surface가 있음을 보여줌

---

## 7. 성능 개선 문서 근거

### 캡처 대상

- [docs/14-performance-lab.md](C:\Users\admin\IdeaProjects\MockStockLive\docs\14-performance-lab.md)
- [docs/20-load-and-dashboard-lab.md](C:\Users\admin\IdeaProjects\MockStockLive\docs\20-load-and-dashboard-lab.md)

### 함께 보이면 좋은 것

- `128 -> 1`
- `42 ms -> < 1 ms`
- `23 -> 2`
- `18 ms -> 13 ms`
- k6 summary table

### 이 스크린샷이 증명해야 하는 것

- 이 프로젝트가 단순 구현이 아니라 before/after 성능 개선 사례를 포함함
- 개선 수치가 README용 과장이 아니라 실제 repo 문서에 남아 있음

---

## 8. 동시성 / 정합성 문서 근거

### 캡처 대상

- [docs/21-concurrency-and-observability-lab.md](C:\Users\admin\IdeaProjects\MockStockLive\docs\21-concurrency-and-observability-lab.md)

### 함께 보이면 좋은 것

- baseline `2` success
- hardened `1` success + `1` failure
- final cash `300.00`

### 이 스크린샷이 증명해야 하는 것

- 동시성 문제가 실제로 재현되었음
- pessimistic locking으로 정합성이 개선되었음
- correctness story가 문서와 수치로 남아 있음

---

## 9. 배포 검증 문서 근거

### 캡처 대상

- [docs/26-vercel-railway-deploy.md](C:\Users\admin\IdeaProjects\MockStockLive\docs\26-vercel-railway-deploy.md)
- [docs/13-troubleshooting.md](C:\Users\admin\IdeaProjects\MockStockLive\docs\13-troubleshooting.md)

### 함께 보이면 좋은 것

- `2026-03-12`
- Vercel + Railway architecture
- custom build/start command
- CORS variable
- deployment troubleshooting entries

### 이 스크린샷이 증명해야 하는 것

- 분리 배포 경험이 실제로 검증되었음
- 배포 과정에서 겪은 문제와 해결이 문서화되어 있음
- 다만 현재도 항상 live라는 과장 없이 "배포 경험을 검증했다"는 수준으로 표현 가능함

---

## 10. README / 전체 프로젝트 요약

### 캡처 대상

- [README.md](C:\Users\admin\IdeaProjects\MockStockLive\README.md) 상단 요약
- key problem-solving cases table

### 함께 보이면 좋은 것

- 프로젝트 개요
- 핵심 기능
- 대표 개선 사례 표

### 이 스크린샷이 증명해야 하는 것

- 프로젝트를 처음 보는 사람도 빠르게 핵심을 파악할 수 있음
- 기능, 개선, 관측, 배포 경험이 한 프로젝트 안에서 연결되어 있음

---

## 추천 캡처 순서

1. README 상단 + 핵심 사례 표
2. stock list / detail
3. buy / sell 직후 화면
4. holdings / trade history
5. SSE connected + 가격 갱신 화면
6. chat join / send / receive
7. actuator / prometheus 메트릭 화면
8. 성능 개선 문서 화면
9. 동시성 문서 화면
10. 배포 문서 + troubleshooting 화면

이 순서대로 정리하면, 포트폴리오 페이지에서 "기능 -> 개선 -> 검증 -> 배포 경험"의 흐름으로 자연스럽게 스토리를 만들 수 있습니다.
