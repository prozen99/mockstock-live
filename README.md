# MockStock Live

MockStock Live is a backend-focused portfolio service that simulates stock trading without external market APIs. The project emphasizes transaction consistency, read-path optimization, real-time communication, and measurable observability rather than feature count alone.

Phase 11 adds a small React + Vite frontend demo so the existing backend flows are easy to verify visually. The project remains backend-first; the frontend is a lightweight local demo layer, not a separate product build.

## Project Overview

- Mock trading with cash balance, holdings, and trade history
- Real-time quote streaming over SSE
- Stock-specific real-time chat over WebSocket/STOMP
- Read-heavy query optimization with explicit before/after measurements
- Monitoring foundation, local load validation, and concurrency correctness documentation

## Why This Project Is Portfolio-Worthy

- It shows both feature delivery and problem-solving, not just CRUD scaffolding.
- It keeps business-critical flows explicit: trading consistency matters more than convenience.
- It includes measurable improvement stories instead of vague performance claims.
- It makes real-time behavior observable through actuator and Prometheus-compatible metrics.
- It documents trade-offs and remaining scope honestly.

## Core Features

- `Auth`: sign up and login flows for local testing
- `Stocks`: stock list and stock detail reads backed by local seed data
- `Trading`: buy, sell, offset history, and cursor history APIs
- `Portfolio`: holdings summary for a user
- `Quotes`: SSE quote stream with optional symbol filtering
- `Chat`: stock room list, room messages, room join, and STOMP chat messaging
- `Observability`: actuator metrics, Prometheus scrape output, custom runtime meters
- `Validation`: local k6 read-load scripts and focused concurrency integration tests
- `Frontend demo`: a small Phase 11 React/Vite UI for local visual verification

## Tech Stack

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
- k6 for local load validation
- React
- Vite

## Architecture Summary

- Layered package structure by domain: `auth`, `stock`, `trading`, `portfolio`, `chat`, `monitoring`
- Thin controllers and explicit service-layer business rules
- DTO-only API responses; JPA entities are not exposed directly
- Separate optimized read paths from simpler command-side flows where needed
- Flyway-managed schema changes and local seed/initializer support
- SSE for one-way quote delivery and WebSocket/STOMP for bidirectional chat
- Actuator and Micrometer instrumentation for trading, read flows, quote fan-out, and chat activity

For a lightweight diagram and module walkthrough, see [docs/24-architecture-overview.md](docs/24-architecture-overview.md).

## Key Problem-Solving Cases

| Case | What was wrong first | Minimal change applied | Measured result |
| --- | --- | --- | --- |
| Chat room list query bottleneck | Service-layer room assembly caused repeated lookups | Replaced repeated per-room access with one projection query | `128` SQL statements to `1`, local average `42 ms` to `< 1 ms` |
| Trade history deep offset pagination | Offset pagination stayed expensive for history-style access | Added cursor endpoint plus `(user_id, id)` index and DTO projection | `23` SQL statements to `2`, local average `18 ms` to `13 ms` |
| Real-time visibility gap | SSE/chat existed but runtime workload shape was hard to explain | Added publish fan-out, latency, and room-subscription metrics | Quote fan-out and hot-room concentration became measurable |
| Concurrent buy correctness risk | Two same-user buy requests could both pass the same balance check | Added pessimistic user-row locking for trade mutations | Baseline `2` successes became hardened `1` success + `1` validation failure with aligned persisted state |

Detailed measurement records:

- [docs/14-performance-lab.md](docs/14-performance-lab.md)
- [docs/19-monitoring-foundation.md](docs/19-monitoring-foundation.md)
- [docs/20-load-and-dashboard-lab.md](docs/20-load-and-dashboard-lab.md)
- [docs/21-concurrency-and-observability-lab.md](docs/21-concurrency-and-observability-lab.md)

## Main API And Real-Time Capabilities

| Area | Capability |
| --- | --- |
| Auth | `POST /api/v1/auth/signup`, `POST /api/v1/auth/login` |
| Stocks | `GET /api/v1/stocks`, `GET /api/v1/stocks/{stockId}` |
| Trading | `POST /api/v1/trades/buy`, `POST /api/v1/trades/sell`, `GET /api/v1/trades/history`, `GET /api/v1/trades/history/cursor` |
| Portfolio | `GET /api/v1/portfolio/holdings` |
| Quotes | `GET /api/v1/quotes/stream`, optional `?symbols=MSL001,MSL003` |
| Chat HTTP | `GET /api/v1/chat/rooms`, `GET /api/v1/chat/rooms/{roomId}/messages`, `POST /api/v1/chat/rooms/{roomId}/join` |
| Chat WebSocket | `CONNECT /ws`, `SUBSCRIBE /sub/chat/rooms/{roomId}`, `SEND /pub/chat/rooms/{roomId}` |
| Operations | `GET /actuator/health`, `GET /actuator/metrics`, `GET /actuator/prometheus` |

The full endpoint list is in [docs/03-api-spec.md](docs/03-api-spec.md).

## Monitoring, Load Validation, And Correctness

- Monitoring foundation exposes actuator metrics plus a Prometheus-compatible scrape endpoint.
- Custom meters track trade requests, trade validation failures, active SSE subscriptions, quote fan-out, quote publish latency, active WebSocket sessions, chat send latency, and room-level subscription skew.
- Local k6 scripts validate stock list, holdings, and trade-history reads.
- Focused concurrency tests reproduce and harden a same-user concurrent buy race.

The most reviewer-relevant docs are:

- [docs/19-monitoring-foundation.md](docs/19-monitoring-foundation.md)
- [docs/20-load-and-dashboard-lab.md](docs/20-load-and-dashboard-lab.md)
- [docs/21-concurrency-and-observability-lab.md](docs/21-concurrency-and-observability-lab.md)

## How To Run Locally

### Prerequisites

- Java 21
- MySQL running locally
- Local secrets configured in `src/main/resources/application-local.yml`
- Node.js LTS if you want to run the Phase 11 frontend demo

Important:
`application-local.yml` is local-only configuration. Do not commit secrets or modify secret management outside your local environment.

### Start The Application

The local profile is already the default active profile in `src/main/resources/application.yml`.

If the current shell does not have Java configured:

```powershell
$env:JAVA_HOME='C:\Users\admin\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Run the app:

```powershell
.\gradlew.bat bootRun
```

If `8080` is already in use:

```powershell
.\gradlew.bat bootRun --args=--server.port=8081
```

### Quick Verification

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/api/v1/stocks
Invoke-WebRequest -Headers @{Accept='text/plain'} http://localhost:8080/actuator/prometheus
```

For chat/STOMP local verification, use `chat-test.html` or your own WebSocket client.

### Run The Frontend Demo

From `frontend/`:

```bash
npm install
npm run dev
```

If the backend runs on a port other than `8080`, create `frontend/.env.local` and set:

```bash
VITE_API_BASE_URL=http://localhost:8081
```

## Reviewer Entry Points

- Portfolio summary: [docs/22-portfolio-summary.md](docs/22-portfolio-summary.md)
- Interview support: [docs/23-interview-qna.md](docs/23-interview-qna.md)
- Architecture overview: [docs/24-architecture-overview.md](docs/24-architecture-overview.md)
- Frontend demo: [docs/25-frontend-demo.md](docs/25-frontend-demo.md)
- API surface: [docs/03-api-spec.md](docs/03-api-spec.md)
- Problem storyboard: [docs/04-problem-scenarios.md](docs/04-problem-scenarios.md)
- Performance lab: [docs/14-performance-lab.md](docs/14-performance-lab.md)

## Current Intentional Limits

- No external market API integration
- No production deployment or infrastructure automation in this phase
- No Redis, Kafka, or distributed real-time fan-out layer
- No authenticated user-context propagation yet; some APIs still use explicit `userId`
- No attempt to solve every possible concurrency case before the core one was measured and hardened

That scope is intentional. The project is designed to show disciplined backend engineering decisions, measurable improvement, and honest trade-offs in a portfolio format.
