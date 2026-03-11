# Interview Q&A

## 1. What is MockStock Live?

MockStock Live is a backend-focused stock simulation service. It supports mock trading, holdings, real-time quote streaming over SSE, stock-specific chat over WebSocket/STOMP, and measurable monitoring and performance work. The portfolio value is in the implementation plus the before/after engineering story.

## 2. Why build it without external stock APIs?

That keeps the project focused on backend design instead of third-party integration noise. I wanted deterministic local behavior so I could measure query cost, transaction correctness, and real-time fan-out with repeatable conditions.

Trade-off:
It does not prove external API resilience or production market-data ingestion.

## 3. Why did you keep controllers thin?

Business rules such as cash validation, holding updates, cursor history logic, and chat membership checks sit in services so the HTTP layer stays stable and easy to review. That also makes problem reproduction and test coverage more direct.

## 4. Why do your APIs use DTOs instead of exposing entities?

DTOs keep the response contract explicit and prevent accidental coupling to JPA mappings or lazy-loading behavior. That matters in a portfolio project because read models and persistence models should be visibly different when their concerns diverge.

## 5. What is the strongest performance case in the project?

The strongest single query case is the chat room list. The initial implementation assembled room data with repeated repository access, which created an N+1-style pattern. I replaced that with one projection query and kept the response shape unchanged. On the Phase 6 dataset, SQL statements dropped from `128` to `1`, and local average latency dropped from `42 ms` to below `1 ms`.

## 6. Why add cursor pagination when the offset endpoint still exists?

I kept the offset endpoint as the baseline so the optimization story stays explainable and backward-compatible. The cursor endpoint is the better long-term contract for append-only trade history, but keeping both made the before/after comparison concrete.

Trade-off:
The API surface is temporarily larger because both history styles exist.

## 7. Why use SSE for quotes and WebSocket/STOMP for chat?

Quotes are one-way server-to-client updates, so SSE is simpler and easier to test from browsers and HTTP clients. Chat needs bidirectional messaging and topic-based room subscriptions, so WebSocket/STOMP is a better fit there.

## 8. What observability work did you actually implement?

I added actuator endpoints, a Prometheus-compatible scrape endpoint, and custom metrics for trade requests, validation failures, quote subscriptions, quote publish fan-out, quote publish latency, chat send latency, active WebSocket sessions, and room-level subscription skew. I also added read-flow metrics and local k6 scripts so read-heavy paths can be compared under burst traffic.

## 9. What was the most important correctness issue you found?

The strongest correctness case was a same-user concurrent buy race. Two requests could both pass the same balance check and produce impossible persisted state. I reproduced that case in a focused integration test, then hardened the trade flow with pessimistic locking on the user row used for trade mutation.

Measured result:
The baseline allowed `2` successful concurrent buys. After hardening, the result became `1` success and `1` validation failure, with cash, holdings, and trade history aligned.

## 10. Why choose pessimistic locking instead of optimistic locking?

For this phase, pessimistic locking was the smallest safe change for the reproduced problem. It made the serialization behavior explicit and easy to explain. Optimistic locking could be a later alternative, but it would require a broader retry/conflict-handling design that was outside the current scope.

Trade-off:
Same-user trade mutations lose some parallelism.

## 11. What did the load validation phase prove?

It proved that the monitoring foundation was useful, not just present. k6 load scripts plus read-flow metrics made it possible to distinguish cheap reads like stock list from heavier history reads and to confirm that the local runs were not limited by connection-pool saturation.

## 12. Did cursor pagination always outperform offset in load tests?

No. Under the moderate local Phase 8 depth, cursor did not beat offset on every latency view. That is an important honest result. The main value of the cursor path is the better contract and scalability headroom for history-style access, not a guarantee of lower latency on every shallow workload.

## 13. Why is that nuance important in an interview?

Because it shows I am not forcing a simplistic optimization claim. I can explain what improved, what did not improve yet, and why the architectural choice still makes sense.

## 14. What was intentionally deferred?

- Full authenticated user-context replacement for explicit `userId`
- Message-history cursor pagination and unread/read-state completion
- Deployment and production infrastructure
- Distributed real-time architecture such as Redis or Kafka
- Broader concurrency cases beyond the reproduced same-user trade mutation scenario

## 15. If you had one more phase, what would you do next?

I would choose one of two narrow directions:

- finish one more measured read-path case, likely chat message history with cursor pagination
- deepen correctness work by covering buy/sell interleaving or concurrent sell cases with the same before/after discipline

I would avoid broadening into deployment or new infrastructure until the next measured backend case is complete.

## Supporting Docs

- [README.md](../README.md)
- [22-portfolio-summary.md](22-portfolio-summary.md)
- [24-architecture-overview.md](24-architecture-overview.md)
- [14-performance-lab.md](14-performance-lab.md)
- [21-concurrency-and-observability-lab.md](21-concurrency-and-observability-lab.md)
