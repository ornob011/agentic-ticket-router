# 08 - Observability

> Monitoring, tracing, and understanding what the system is doing.

---

## Table of Contents

1. [Why Observability?](#1-why-observability)
2. [Run Tracking](#2-run-tracking)
3. [Step Tracing](#3-step-tracing)
4. [SSE Progress Streaming](#4-sse-progress-streaming)
5. [Logging Strategy](#5-logging-strategy)
6. [Querying Traces](#6-querying-traces)

---

## 1. Why Observability?

### The Challenge

```
┌────────────────────────────────────────────────────────────────┐
│                    THE OBSERVABILITY CHALLENGE                 │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   AI systems are "black boxes":                                │
│                                                                │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │                                                         │  │
│   │   Questions you need to answer:                         │  │
│   │                                                         │  │
│   │   • Why did this ticket route to TECH_Q?                │  │
│   │   • What was the LLM's confidence?                      │  │
│   │   • Which policy rule triggered HUMAN_REVIEW?           │  │
│   │   • How long did the LLM take to respond?               │  │
│   │   • Why did the fallback get used?                      │  │
│   │   • What was the raw LLM output?                        │  │
│   │   • How many steps did the state machine take?          │  │
│   │   • Is the agent runtime performing well?               │  │
│   │                                                         │  │
│   └─────────────────────────────────────────────────────────┘  │
│                                                                │
│   Without observability:                                       │
│   • Can't debug routing decisions                              │
│   • Can't improve the system                                   │
│   • Can't respond to customer complaints                       │
│   • Can't detect problems early                                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Our Solution

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY PILLARS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. RUN TRACKING                                               │
│      • Every execution recorded in agent_runtime_run            │
│      • Status, duration, termination reason                     │
│      • Link to all steps                                        │
│                                                                 │
│   2. STEP TRACING                                               │
│      • Every node execution recorded in agent_runtime_step      │
│      • Input/output at each step                                │
│      • Latency per step                                         │
│                                                                 │
│   3. LLM OUTPUT LOGGING                                         │
│      • Full prompt and response in llm_output                   │
│      • Parse status and errors                                  │
│      • Model used, latency                                      │
│                                                                 │
│   4. REAL-TIME PROGRESS                                         │
│      • SSE streaming to frontend                                │
│      • Live updates during execution                            │
│      • Completion events                                        │
│                                                                 │
│   5. STRUCTURED LOGGING                                         │
│      • Consistent log format                                    │
│      • Correlation IDs                                          │
│      • Searchable patterns                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Run Tracking

### agent_runtime_run Table

```
┌─────────────────────────────────────────────────────────────────┐
│                          RUN TRACKING                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Table: agent_runtime_run                                      │
│                                                                 │
│   ┌───────────────────┬────────────┬────────────────────────┐   │
│   │ Column            │ Type       │ Description            │   │
│   ├───────────────────┼────────────┼────────────────────────┤   │
│   │ id                │ BIGINT     │ Primary key            │   │
│   │ ticket_id         │ BIGINT     │ Which ticket           │   │
│   │ status            │ ENUM       │ RUNNING/COMPLETED/etc  │   │
│   │ termination_reason│ ENUM       │ Why ended              │   │
│   │ total_steps       │ INT        │ Steps executed         │   │
│   │ started_at        │ TIMESTAMP  │ When started           │   │
│   │ ended_at          │ TIMESTAMP  │ When ended             │   │
│   │ fallback_used     │ BOOLEAN    │ Did fallback trigger?  │   │
│   │ error_code        │ VARCHAR    │ Error if failed        │   │
│   │ error_message     │ TEXT       │ Human-readable error   │   │
│   └───────────────────┴────────────┴────────────────────────┘   │
│                                                                 │
│   Status Values:                                                │
│   ──────────────                                                │
│   • RUNNING    - Currently executing                            │
│   • COMPLETED  - Finished successfully                          │
│   • FAILED     - Ended with error                               │
│                                                                 │
│   Termination Reasons:                                          │
│   ────────────────────                                          │
│   • GOAL_REACHED           - Action completed                   │
│   • SAFETY_BLOCKED         - Policy gate triggered              │
│   • BUDGET_EXCEEDED        - Step/time limit reached            │
│   • PLAN_VALIDATION_FAILED - LLM output invalid                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Run Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                          RUN LIFECYCLE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Timeline:                                                     │
│                                                                 │
│   t=0s    ┌───────────────────────────────────────────────────┐ │
│           │ START RUN                                         │ │
│           │                                                   │ │
│           │ INSERT INTO agent_runtime_run (                   │ │
│           │   ticket_id,                                      │ │
│           │   status = 'RUNNING',                             │ │
│           │   started_at = now()                              │ │
│           │ )                                                 │ │
│           └───────────────────────────────────────────────────┘ │
│                                                                 │
│   t=0-10s ┌───────────────────────────────────────────────────┐ │
│           │ EXECUTE STATE MACHINE                             │ │
│           │                                                   │ │
│           │ Each step records to agent_runtime_step           │ │
│           │                                                   │ │
│           └───────────────────────────────────────────────────┘ │
│                                                                 │
│   t=10s   ┌───────────────────────────────────────────────────┐ │
│           │ END RUN                                           │ │
│           │                                                   │ │
│           │ UPDATE agent_runtime_run SET                      │ │
│           │   status = 'COMPLETED',                           │ │
│           │   termination_reason = 'GOAL_REACHED',            │ │
│           │   total_steps = 5,                                │ │
│           │   ended_at = now()                                │ │
│           │ WHERE id = ?                                      │ │
│           └───────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Step Tracing

### agent_runtime_step Table

```
┌─────────────────────────────────────────────────────────────────┐
│                          STEP TRACING                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Table: agent_runtime_step                                     │
│                                                                 │
│   ┌───────────────────┬────────────┬────────────────────────┐   │
│   │ Column            │ Type       │ Description            │   │
│   ├───────────────────┼────────────┼────────────────────────┤   │
│   │ id                │ BIGINT     │ Primary key            │   │
│   │ run_id            │ BIGINT     │ FK to agent_run        │   │
│   │ step_no           │ INT        │ Sequence (1, 2, 3...)  │   │
│   │ step_type         │ ENUM       │ PLAN/SAFETY/ACT/etc    │   │
│   │ planner_output    │ JSONB      │ Raw LLM JSON           │   │
│   │ validated_output  │ JSONB      │ Cleaned/Parsed JSON    │   │
│   │ safety_decision   │ JSONB      │ Policy results         │   │
│   │ tool_result       │ JSONB      │ API/Function response  │   │
│   │ latency_ms        │ BIGINT     │ Execution time         │   │
│   │ success           │ BOOLEAN    │ Step completion status │   │
│   │ actor_role        │ VARCHAR    │ Executing agent        │   │
│   │ target_role       │ VARCHAR    │ Delegated agent        │   │
│   │ handoff_reason    │ TEXT       │ Why switch roles?      │   │
│   └───────────────────┴────────────┴────────────────────────┘   │
│                                                                 │
│   Step Types:                                                   │
│   ───────────                                                   │
│   • PLAN    - The LLM deciding what to do next                  │
│   • SAFETY  - The Policy Engine gatekeeper                      │
│   • ACT     - Calling a Tool (API, DB, Search)                  │
│   • REFLECT - Self-correction or final review                   │
│   • HANDOFF - Transferring control to a specialist              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Step Example

```
┌┌─────────────────────────────────────────────────────────────────┐
│                       EXAMPLE STEP RECORD                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Step 1 - PLAN:                                                │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "run_id": 1001,                                       │   │
│   │   "step_no": 1,                                         │   │
│   │   "step_type": "PLAN",                                  │   │
│   │   "planner_output": {                                   │   │
│   │     "raw": "{\"category\":\"BILLING\"...}",             │   │
│   │     "parsed": {...}                                     │   │
│   │   },                                                    │   │
│   │   "validated_response": {                               │   │
│   │     "category": "BILLING",                              │   │
│   │     "priority": "HIGH",                                 │   │
│   │     "queue": "BILLING_Q",                               │   │
│   │     "nextAction": "AUTO_REPLY",                         │   │
│   │     "confidence": 0.92,                                 │   │
│   │     "draftReply": "Hi! To update your payment..."       │   │
│   │   },                                                    │   │
│   │   "latency_ms": 2340,                                   │   │
│   │   "success": true,                                      │   │
│   │   "actor_role": "SUPERVISOR",                           │   │
│   │   "target_role": "RESOLVER",                            │   │
│   │   "handoff": true,                                      │   │
│   │   "handoff_reason": "Supervisor delegation"             │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Step 2 - SAFETY:                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "run_id": 1001,                                       │   │
│   │   "step_no": 2,                                         │   │
│   │   "step_type": "SAFETY",                                │   │
│   │   "safety_decision": {                                  │   │
│   │     "status": "ALLOW",                                  │   │
│   │     "policyOverridden": false,                          │   │
│   │     "policyReasons": []                                 │   │
│   │   },                                                    │   │
│   │   "latency_ms": 5,                                      │   │
│   │   "success": true                                       │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. SSE Progress Streaming

### What Is SSE?

```
┌─────────────────────────────────────────────────────────────────┐
│                    SERVER-SENT EVENTS (SSE)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   SSE provides real-time updates from server to client:         │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Client (Browser)                                      │   │
│   │        │                                                │   │
│   │        │ GET /api/v1/tickets/123/routing-progress       │   │
│   │        │ Accept: text/event-stream                      │   │
│   │        ▼                                                │   │
│   │   Server                                                │   │
│   │        │                                                │   │
│   │        │ event: progress                                │   │
│   │        │ data: {"node":"PLAN","status":"started"}       │   │
│   │        │                                                │   │
│   │        │ event: progress                                │   │
│   │        │ data: {"node":"PLAN","status":"completed"}     │   │
│   │        │                                                │   │
│   │        │ event: progress                                │   │
│   │        │ data: {"node":"SAFETY","status":"started"}     │   │
│   │        │                                                │   │
│   │        │ ...                                            │   │
│   │        │                                                │   │
│   │        │ event: complete                                │   │
│   │        │ data: {"action":"AUTO_REPLY"}                  │   │
│   │        ▼                                                │   │
│   │   Client receives updates in real-time                  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Progress Events

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROGRESS EVENT FORMAT                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Event Types:                                                  │
│                                                                 │
│   progress - Node started/completed                             │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "ticketId": 123,                                      │   │
│   │   "node": "PLAN",                                       │   │
│   │   "status": "started",                                  │   │
│   │   "message": "Planning routing decision..."             │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   complete - Execution finished                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "ticketId": 123,                                      │   │
│   │   "action": "AUTO_REPLY",                               │   │
│   │   "queue": "BILLING_Q",                                 │   │
│   │   "confidence": 0.92                                    │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   error - Execution failed                                      │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "ticketId": 123,                                      │   │
│   │   "error": "MODEL_ERROR",                               │   │
│   │   "message": "LLM response timeout"                     │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Logging Strategy

### Structured Log Format

```
┌─────────────────────────────────────────────────────────────────┐
│                        LOGGING PATTERNS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Log Format:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   [TIMESTAMP] [LEVEL] [CORRELATION_ID] Message          │   │
│   │                                                         │   │
│   │   Example:                                              │   │
│   │                                                         │   │
│   │   2024-01-15 10:05:23.456 INFO  [abc123]                │   │
│   │   TicketRoute(start) SupportTicket(id:123)              │   │
│   │                                                         │   │
│   │   2024-01-15 10:05:25.890 INFO  [abc123]                │   │
│   │   AgentRuntime(decision) SupportTicket(id:123)          │   │
│   │   Outcome(source:AGENT_RUNTIME,nextAction:AUTO_REPLY)   │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Key Log Patterns to Search:                                   │
│   ──────────────────────────                                    │
│                                                                 │
│   Start of routing:                                             │
│   "TicketRoute(start) SupportTicket"                            │
│                                                                 │
│   Completion:                                                   │
│   "TicketRoute(complete)"                                       │
│                                                                 │
│   Policy triggered:                                             │
│   "PolicyRule(triggered)"                                       │
│                                                                 │
│   Fallback used:                                                │
│   "RoutingFallback(fail)"                                       │
│                                                                 │
│   Circuit opened:                                               │
│   "CircuitBreaker(open)"                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Querying Traces

### Useful SQL Queries

```
┌─────────────────────────────────────────────────────────────────┐
│                       DIAGNOSTIC QUERIES                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Find all runs for a ticket:                                   │
│   ───────────────────────────                                   │
│   SELECT * FROM agent_runtime_run                               │
│   WHERE ticket_id = 123                                         │
│   ORDER BY started_at DESC;                                     │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Get all steps for a run:                                      │
│   ────────────────────────                                      │
│   SELECT step_no, step_type, latency_ms, success                │
│   FROM agent_runtime_step                                       │
│   WHERE run_id = 1001                                           │
│   ORDER BY step_no;                                             │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Find failed runs:                                             │
│   ─────────────────                                             │
│   SELECT r.*, t.ticket_no, r.error_message                      │
│   FROM agent_runtime_run r                                      │
│   JOIN support_ticket t ON r.ticket_id = t.id                   │
│   WHERE r.status = 'FAILED'                                     │
│   ORDER BY r.started_at DESC                                    │
│   LIMIT 20;                                                     │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Average latency by step type:                                 │
│   ─────────────────────────────                                 │
│   SELECT step_type,                                             │
│          AVG(latency_ms) as avg_latency,                        │
│          COUNT(*) as count                                      │
│   FROM agent_runtime_step                                       │
│   WHERE created_at > NOW() - INTERVAL '24 hours'                │
│   GROUP BY step_type;                                           │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Runs requiring human review:                                  │
│   ────────────────────────────                                  │
│   SELECT r.*, s.safety_decision                                 │
│   FROM agent_runtime_run r                                      │
│   JOIN agent_runtime_step s ON r.id = s.run_id                  │
│   WHERE s.step_type = 'SAFETY'                                  │
│   AND s.safety_decision->>'status' = 'REQUIRES_HUMAN_REVIEW'    │
│   ORDER BY r.started_at DESC;                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [07 - Resilience](./07-resilience.md)  
**Next:** [09 - Configuration](./09-configuration.md)
