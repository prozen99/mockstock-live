# Phase Case Study Template

Use this template when converting a completed phase case into README or portfolio prose.

---

## 1. Case Title
- One sentence title:

## 2. Why It Mattered
- What user-facing or system-level risk existed?
- Why was this worth prioritizing in this phase?

## 3. Initial Implementation
- Which endpoint or flow was involved?
- What was the original query or transaction shape?
- Why was that initial approach reasonable at first?

## 4. Reproduction Setup
- Data volume:
- Request pattern:
- Local environment notes:
- Measurement tool:

## 5. Baseline Measurement
- SQL statement count:
- Latency:
- `EXPLAIN` or query-plan note:
- Correctness symptom, if applicable:
- Concurrency symptom, if applicable:
  for example, "two requests succeeded even though only one should have been valid"

## 6. Root Cause
- What specifically made the baseline slow or risky?
- Keep this to one or two direct technical sentences.

## 7. Minimal Change Applied
- What code or query changed?
- What did you intentionally not change?

## 8. After Measurement
- SQL statement count:
- Latency:
- Query-plan note:
- Correctness result:
- Concurrency result:
  for example, "one request succeeded, one failed, and final persisted state stayed aligned"

## 9. Before / After Summary
| Metric | Before | After | Note |
|---|---:|---:|---|
| SQL statements |  |  |  |
| Latency |  |  |  |
| Plan / rows |  |  |  |
| Correctness |  |  |  |

## 10. Remaining Limitations
- What is still not solved?
- What should be deferred to the next phase?

## 11. Interview-Ready Summary
- Problem:
- Change:
- Result:

## 12. Source References
- Code files:
- Test or reproduction files:
- Related docs:
