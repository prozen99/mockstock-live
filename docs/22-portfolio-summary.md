# Portfolio Summary

## Project Positioning

MockStock Live is a backend portfolio project built to show more than CRUD delivery. It simulates a stock-trading service with real-time quote streaming, stock chat, measurable read-path optimization, and explicit concurrency hardening, all on a local-friendly stack without external market dependencies.

The project is positioned as:

- a transaction-focused backend service
- a read-optimization case study
- a real-time communication example using two different protocols
- an observability-first portfolio piece that can explain trade-offs with measurements

## Why This Project Is Portfolio-Worthy

- It combines consistency-sensitive writes with read-heavy optimization work.
- It includes both HTTP and real-time communication paths.
- It records before/after performance changes with concrete numbers.
- It treats monitoring and correctness as implemented work, not future ideas.
- It is documented so a reviewer can understand the strongest cases quickly.

## Major Phases Completed

| Phase | Outcome |
| --- | --- |
| Phase 1 | Project foundation, MySQL connection, Flyway baseline, shared package structure |
| Phase 2 | Auth and stock read foundations |
| Phase 3 | Trading and holdings flows with explicit business validation |
| Phase 4 | Mock quote generation and SSE quote streaming |
| Phase 5 | Stock chat rooms and WebSocket/STOMP messaging |
| Phase 6 | Read-path bottleneck reproduction and optimization for chat room list and trade history |
| Phase 7 | Monitoring foundation with actuator and Prometheus-compatible metrics |
| Phase 8 | Local read-load validation with k6 and flow-level read metrics |
| Phase 9 | Real-time observability extension and concurrent buy correctness hardening |

## Most Important Technical Decisions

### 1. DTO-only API boundaries
JPA entities are not exposed directly. This keeps API contracts explicit and avoids leaking persistence shape into the HTTP layer.

### 2. Different strategies for command and read-heavy paths
Simple flows stay simple, but read-heavy endpoints use dedicated projection queries or cursor contracts when the initial approach becomes measurably weak.

### 3. SSE for quotes, WebSocket/STOMP for chat
Quote delivery is one-way fan-out, so SSE keeps the client flow simple. Chat is bidirectional, so WebSocket/STOMP is a better fit for room messaging.

### 4. Measure before claiming optimization
The project keeps the intentionally weak first approach visible, reproduces the problem, and then documents the smallest meaningful improvement.

### 5. Favor explainable correctness over speculative complexity
For the reproduced same-user trading race, pessimistic locking was chosen because it is easy to reason about and directly fixes the measured integrity problem.

## Strongest Measurable Achievements

| Case | Before | After | Why it matters |
| --- | --- | --- | --- |
| Chat room list query path | `128` SQL statements, `42 ms` local average | `1` SQL statement, `< 1 ms` local average | Strong projection-query example on a read-heavy screen |
| Trade history deep-page access | `23` SQL statements, `18 ms` local average | `2` SQL statements, `13 ms` local average | Shows migration from convenient offset pagination to cursor design |
| Real-time quote visibility | Publish cycles and subscriber count only | Delivered events, recipients per cycle, publish latency visible | Makes SSE workload shape explainable |
| Concurrent buy correctness | `2` same-user buys both succeeded incorrectly | `1` success and `1` validation failure, persisted state aligned | Demonstrates transaction integrity under concurrency |

Primary measurement references:

- [14-performance-lab.md](14-performance-lab.md)
- [20-load-and-dashboard-lab.md](20-load-and-dashboard-lab.md)
- [21-concurrency-and-observability-lab.md](21-concurrency-and-observability-lab.md)

## What Makes The Project Interview-Ready

- There are clear stories for architecture, performance, observability, and correctness.
- Trade-offs are explicit instead of hidden.
- Remaining gaps are documented, so the project reads as intentionally scoped rather than incomplete by accident.
- Reviewers can start from the README and move to focused supporting docs quickly.

## What Remains Intentionally Out Of Scope

- External broker or market-data integration
- Deployment pipeline or cloud infrastructure work
- Redis/Kafka-based distributed real-time architecture
- Full authenticated user-context replacement for temporary explicit `userId` parameters
- Broad concurrency redesign beyond the reproduced same-user trade mutation case
- Frontend product surface beyond lightweight local verification artifacts

## Recommended Review Order

1. [README.md](../README.md)
2. [24-architecture-overview.md](24-architecture-overview.md)
3. [14-performance-lab.md](14-performance-lab.md)
4. [21-concurrency-and-observability-lab.md](21-concurrency-and-observability-lab.md)
5. [23-interview-qna.md](23-interview-qna.md)
