# AGENTS.md

## Project identity
This project is a backend-focused portfolio service.

Project name:
MockStock Live

Core features:
- mock stock trading without external APIs
- real-time quote streaming
- stock-specific real-time chat
- measurable query optimization
- monitoring and load testing
- production-minded architecture

## Primary goals
This project does NOT prioritize fancy patterns first.
This project prioritizes:
1. transaction consistency
2. DB query tuning
3. network protocol design
4. observability
5. measurable performance improvement

## Mandatory rules
- Never expose JPA entities directly in API responses.
- Always use DTOs.
- Separate simple CRUD from read-heavy optimized queries.
- For large history/list endpoints, prefer keyset pagination.
- Always inspect query access patterns before optimizing.
- Before considering optimization complete, compare before/after.
- If schema changes, add migration.
- If a feature is performance-sensitive, add metrics or at least measurement notes.
- Keep controller thin and business rules explicit.

## Network rules
- Use SSE for quote streaming and one-way notifications.
- Use WebSocket/STOMP for bidirectional chat.
- Keep payloads small and explicit.

## Portfolio rules
This is a portfolio project.
So the codebase and docs must include:
- intentionally problematic first approach
- why it becomes a problem
- how it was measured
- how it was improved
- what metric changed after the fix

## Delivery rules
For any meaningful task:
1. inspect related files
2. explain the problem briefly
3. implement the smallest safe change
4. mention performance implications
5. summarize changed files

- Read docs/11-commit-rules.md before creating commits.
- Read docs/12-phase-rules.md before starting a new implementation step.