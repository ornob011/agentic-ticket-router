# 10 - Troubleshooting

> Common issues, debugging tools, and error reference.

---

## Table of Contents

1. [Common Issues](#1-common-issues)
2. [Debugging Tools](#2-debugging-tools)
3. [Error Code Reference](#3-error-code-reference)
4. [Diagnostic Queries](#4-diagnostic-queries)

---

## 1. Common Issues

### Issue: Routing Stuck in TRIAGING

```
┌─────────────────────────────────────────────────────────────────┐
│                    TICKET STUCK IN TRIAGING                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Symptom:                                                      │
│   Ticket status remains 'TRIAGING' indefinitely                 │
│                                                                 │
│   Possible Causes:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. LLM timeout (30+ seconds)                          │   │
│   │      → Check: agent_runtime_run where status = 'RUNNING'│   │
│   │                                                         │   │
│   │   2. Circuit breaker OPEN                               │   │
│   │      → Check: Prometheus / Resilience4j metrics         │   │
│   │                                                         │   │
│   │   3. Exception not caught properly                      │   │
│   │      → Check: Application logs for Stack Traces         │   │
│   │                                                         │   │
│   │   4. Database lock                                      │   │
│   │      → Check: Postgres pg_stat_activity                 │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Resolution:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Find stuck tickets:                                │   │
│   │      SELECT * FROM support_ticket                       │   │
│   │      WHERE status = 'TRIAGING'                          │   │
│   │      AND updated_at < NOW() - INTERVAL '5 minutes';     │   │
│   │                                                         │   │
│   │   2. Check for running agent runs:                      │   │
│   │      SELECT * FROM agent_runtime_run                    │   │
│   │      WHERE status = 'RUNNING';                          │   │
│   │                                                         │   │
│   │   3. Manually mark for human review:                    │   │
│   │      UPDATE support_ticket                              │   │
│   │      SET status = 'ASSIGNED',                           │   │
│   │          requires_human_review = true                   │   │
│   │      WHERE id = ?;                                      │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Issue: LLM Returns Invalid JSON

```
┌─────────────────────────────────────────────────────────────────┐
│                    INVALID JSON FROM LLM                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Symptom:                                                      │
│   Schema validation fails, fallback to HUMAN_REVIEW             │
│                                                                 │
│   Possible Causes:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. LLM model too weak/small                           │   │
│   │                                                         │   │
│   │   2. Prompt too complex                                 │   │
│   │                                                         │   │
│   │   3. Temperature too high                               │   │
│   │                                                         │   │
│   │   4. Response truncated (token limit)                   │   │
│   │                                                         │   │
│   │   5. LLM hallucinating format                           │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Resolution:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Check raw LLM output:                              │   │
│   │      SELECT raw_response FROM llm_output                │   │
│   │      WHERE ticket_id = ? ORDER BY created_at DESC;      │   │
│   │                                                         │   │
│   │   2. If repair enabled, check repair attempts:          │   │
│   │      SELECT * FROM agent_runtime_step                   │   │
│   │      WHERE step_type = 'PLAN' AND run_id = ?;           │   │
│   │                                                         │   │
│   │   3. Consider:                                          │   │
│   │      • Increase planner-validation-retries              │   │
│   │      • Lower temperature (target 0.0 for logic)         │   │
│   │      • Simplify prompt (few-shot examples)              │   │
│   │      • Switch to a largert model                        │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Issue: Circuit Breaker Keeps Opening

```
┌─────────────────────────────────────────────────────────────────┐
│                    CIRCUIT BREAKER OPENING                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Symptom:                                                      │
│   All routing falls back immediately                            │
│                                                                 │
│   Possible Causes:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. LLM service down                                   │   │
│   │                                                         │   │
│   │   2. Network issues                                     │   │
│   │                                                         │   │
│   │   3. LLM rate limiting (429 errors)                     │   │
│   │                                                         │   │
│   │   4. Authentication issues (401/403)                    │   │
│   │                                                         │   │
│   │   5. Timeout too short                                  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Resolution:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Check LLM service health:                          │   │
│   │      curl http://localhost:11434/api/tags               │   │
│   │                                                         │   │
│   │   2. Check recent LLM outputs for errors:               │   │
│   │      SELECT * FROM llm_output                           │   │
│   │      WHERE parse_status = 'MODEL_ERROR'                 │   │
│   │      ORDER BY created_at DESC LIMIT 20;                 │   │
│   │                                                         │   │
│   │   3. Verify API credentials                             │   │
│   │                                                         │   │
│   │   4. Increase timeout if needed                         │   │
│   │                                                         │   │
│   │   5. Wait for circuit to close (30s default)            │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Issue: High Latency on Routing

```
┌─────────────────────────────────────────────────────────────────┐
│                          HIGH LATENCY                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Symptom:                                                      │
│   Routing takes 30+ seconds                                     │
│                                                                 │
│   Possible Causes:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Slow Token Generation (Output Length)              │   │
│   │                                                         │   │
│   │   2. Large Context (Input Bottleneck)                   │   │
│   │                                                         │   │
│   │   3. Vector Search / Reranking Overhead                 │   │
│   │                                                         │   │
│   │   4. Cold Starts / Infrastructure Bloat                 │   │
│   │                                                         │   │
│   │   5. Sequential Execution (Blocking Calls)              │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Resolution:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Audit Step Latencies:                              │   │
│   │      SELECT step_type, AVG(latency_ms) as avg           │   │
│   │      FROM agent_runtime_step                            │   │
│   │      GROUP BY step_type;                                │   │
│   │                                                         │   │
│   │   2. Implement Context Pruning:                         │   │
│   │      • Truncate history via sliding window              │   │
│   │      • Summarize old messages before sending            │   │
│   │                                                         │   │
│   │   3. Use Prompt Caching:                                │   │
│   │      • Keep static instructions at the start            │   │
│   │      • Moves dynamic data (RAG) to the end              │   │
│   │                                                         │   │
│   │   4. Parallelize Tools:                                 │   │
│   │      • Call vector search and DB lookups in parallel    │   │
│   │                                                         │   │
│   │   5. Streaming UI:                                      │   │
│   │      • Show "Chain of Thought" as it generates          │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Debugging Tools

### Log Search Patterns

```
┌─────────────────────────────────────────────────────────────────┐
│                    LOG SEARCH PATTERNS                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Find routing start:                                           │
│   grep "TicketRoute(start)" application.log                     │
│                                                                 │
│   Find routing completion:                                      │
│   grep "TicketRoute(complete)" application.log                  │
│                                                                 │
│   Find fallback usage:                                          │
│   grep "RoutingFallback" application.log                        │
│                                                                 │
│   Find policy triggered:                                        │
│   grep "PolicyRule.*triggered" application.log                  │
│                                                                 │
│   Find schema validation failure:                               │
│   grep "PlanValidationFailed" application.log                   │
│                                                                 │
│   Find specific ticket:                                         │
│   grep "SupportTicket(id:123)" application.log                  │
│                                                                 │
│   Find circuit breaker events:                                  │
│   grep "CircuitBreaker" application.log                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### SSE Endpoint Testing

```bash
# Test SSE progress endpoint
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/api/v1/tickets/123/routing-progress

# Should see events like:
# event: progress
# data: {"ticketId":123,"node":"PLAN","status":"started"}
```

---

## 3. Error Code Reference

### AgentValidationErrorCode

```
┌─────────────────────────────────────────────────────────────────┐
│                    VALIDATION ERROR CODES                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Code                      │ Meaning                           │
│   ══════════════════════════╪═════════════════════════════════  │
│   MISSING_REQUIRED_FIELD    │ Required field not in response    │
│   INVALID_CATEGORY          │ Category not in allowed values    │
│   INVALID_PRIORITY          │ Priority not in allowed values    │
│   INVALID_QUEUE             │ Queue not in allowed values       │
│   INVALID_NEXT_ACTION       │ Action not in allowed values      │
│   CONFIDENCE_OUT_OF_RANGE   │ Confidence not 0.0-1.0            │
│   SCHEMA_VIOLATION          │ JSON doesn't match schema         │
│   PARSE_ERROR               │ Couldn't parse JSON at all        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### AgentTerminationReason

```
┌─────────────────────────────────────────────────────────────────┐
│                    TERMINATION REASONS                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Reason                    │ Meaning                           │
│   ══════════════════════════╪═════════════════════════════════  │
│   GOAL_REACHED              │ Successfully completed action     │
│   SAFETY_BLOCKED            │ Policy gate forced stop           │
│   BUDGET_EXCEEDED           │ Step/time limit reached           │
│   PLAN_VALIDATION_FAILED    │ LLM output couldn't be repaired   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### ParseStatus

```
┌─────────────────────────────────────────────────────────────────┐
│                    PARSE STATUS                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Status                    │ Meaning                           │
│   ══════════════════════════╪═════════════════════════════════  │
│   SUCCESS                   │ Parsed correctly                  │
│   VALIDATION_ERROR          │ Valid JSON, invalid values        │
│   PARSE_ERROR               │ Invalid JSON                      │
│   TIMEOUT                   │ LLM timed out                     │
│   MODEL_ERROR               │ LLM service error                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Diagnostic Queries

### Quick Diagnostics

```sql
-- Recent routing failures
SELECT t.ticket_no, r.status, r.error_code, r.error_message, r.started_at
FROM agent_runtime_run r
         JOIN support_ticket t ON r.ticket_id = t.id
WHERE r.status = 'FAILED'
ORDER BY r.started_at DESC LIMIT 10;

-- Tickets requiring human review
SELECT t.ticket_no, t.current_category, t.assigned_queue, s.validated_response ->>'confidence' as confidence
FROM support_ticket t
    JOIN ticket_routing tr
ON t.id = tr.ticket_id
    JOIN agent_runtime_step s ON s.validated_response IS NOT NULL
WHERE t.requires_human_review = true
ORDER BY t.updated_at DESC
    LIMIT 10;

-- Average latency by step
SELECT step_type,
       COUNT(*) as count,
       AVG(latency_ms) as avg_ms,
       MAX(latency_ms) as max_ms
FROM agent_runtime_step
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY step_type
ORDER BY avg_ms DESC;

-- Policy trigger frequency
SELECT s.safety_decision - > 'policyReasons' as reasons,
       COUNT(*) as count
FROM agent_runtime_step s
WHERE s.step_type = 'SAFETY'
  AND s.safety_decision-
    >'policyReasons' != '[]'
GROUP BY s.safety_decision->'policyReasons'
ORDER BY count DESC;

-- Fallback usage rate
SELECT COUNT(CASE WHEN fallback_used THEN 1 END)                              as fallback_count,
       COUNT(*)                                                               as total_count,
       ROUND(100.0 * COUNT(CASE WHEN fallback_used THEN 1 END) / COUNT(*), 2) as fallback_rate
FROM agent_runtime_run
WHERE started_at > NOW() - INTERVAL '24 hours';
```

---

## Navigation

**Previous:** [09 - Configuration](./09-configuration.md)  
**Next:** [11 - Performance Tuning](./11-performance-tuning.md)
