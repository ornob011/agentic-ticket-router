# Agentic Ticket Router: Technical Overview

> A 30-minute technical deep-dive into building production-ready AI agents with LangGraph4j

---

## Agenda

```
┌───────────────────────────────────────────────────────────┐
│  SECTION                                   TIME           │
├───────────────────────────────────────────────────────────┤
│  1. What Is It?                           3 min           │
│  2. Why Agentic?                          4 min           │
│  3. Architecture                          5 min           │
│  4. State Machine (LangGraph4j)           5 min           │
│  5. Rule-Based System & Safety Rules      4 min           │
│  6. JSON Validation & Auto-Repair         3 min           │
│  7. Single vs Multi-Agent Orchestration   4 min           │
│  8. Resilience & Key Takeaways            2 min           │
└───────────────────────────────────────────────────────────┘
```

---

## 1. What Is Agentic Ticket Router?

An **AI-powered support ticket routing system** that uses autonomous agents to classify, prioritize, and route support
tickets.

### Core Capabilities

| Capability            | What It Does                                | Business Impact                            |
|-----------------------|---------------------------------------------|--------------------------------------------|
| Auto-routing          | Classifies and assigns to correct queue     | Reduces manual triage by 70%+              |
| Auto-reply            | Generates draft responses for simple issues | Resolves tickets without agent involvement |
| Priority detection    | Identifies urgent issues automatically      | Critical issues get attention fast         |
| Security escalation   | Detects security-related content            | Automatic escalation to security team      |
| Clarifying questions  | Asks for more info when ambiguous           | Improves ticket quality                    |
| Human review triggers | Flags low-confidence decisions              | Prevents AI mistakes reaching customers    |

### Technology Stack

```
┌─────────────────────────────────────────────────────────────────┐
│  LAYER           │ TECHNOLOGY           │ PURPOSE               │
├─────────────────────────────────────────────────────────────────┤
│  State Machine   │ LangGraph4j          │ Multi-step reasoning  │
│  LLM Integration │ Spring AI            │ Unified LLM interface │
│  Inference       │ Ollama / OpenAI      │ Local or cloud LLM    │
│  Vector Store    │ pgvector             │ RAG knowledge base    │
│  Resilience      │ Resilience4j         │ Circuit breaker, retry│
│  Real-time       │ SSE                  │ Progress streaming    │
│  Database        │ PostgreSQL + JSONB   │ Persistence, vectors  │
│  Framework       │ Spring Boot 3.5      │ DI, transactions      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Why Agentic?

### Traditional LLM vs Agentic

```
┌───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  TRADITIONAL LLM APP                              │  AGENTIC SYSTEM                                       │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                   │                                                       │
│  User Input                                       │  User Input                                           │
│      │                                            │      │                                                │
│      ▼                                            │      ▼                                                │
│  Prompt + LLM                                     │  Context + Memory Enrichment                          │
│      │                                            │      │                                                │
│      ▼                                            │      ▼                                                │
│  Single Response                                  │  Planner (structured decision)                        │
│      │                                            │      │                                                │
│      ▼                                            │      ▼                                                │
│  Return Output                                    │  Safety + Policy Gates                                │
│                                                   │      │                                                │
│  Typical limits:                                  │      ▼                                                │
│  • One-shot reasoning                             │  Tool / Action Execution                              │
│  • Limited verification                           │      │                                                │
│  • No multi-step control loop                     │      ▼                                                │
│  • Weak fallback behavior                         │  Reflect / Evaluate                                   │
│                                                   │      │                                                │
│                                                   │      ├────────── loop back to Planner if needed       │
│                                                   │      ▼                                                │
│                                                   │  Terminate with validated final response              │
│                                                   │      │                                                │
│                                                   │      ▼                                                │
│                                                   │  Trace + Monitoring + Streaming progress              │
│                                                   │                                                       │
│                                                   │  Reliability traits:                                  │
│                                                   │  • Multi-step control loop                            │
│                                                   │  • Policy/safety enforcement                          │
│                                                   │  • Tool-backed actions                                │
│                                                   │  • Structured fallback path                           │
└───────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Comparison Matrix

| Feature           | Traditional LLM | Agentic System                     |
|-------------------|-----------------|------------------------------------|
| **Reasoning**     | Single shot     | Multi-step state machine           |
| **Memory**        | None            | State persists across steps        |
| **Validation**    | None            | JSON schema + auto-repair          |
| **Guardrails**    | None            | Policy engine with ordered rules   |
| **Actions**       | None            | Tool execution layer               |
| **Observability** | Logs only       | Full run/step tracing              |
| **Agents**        | Single          | Supervisor → Specialist delegation |
| **Safety Limits** | None            | Step count, timeout, action budget |
| **Fallback**      | None            | Automatic human review             |
| **Recovery**      | Manual          | Auto-repair with retry             |

---

## 3. Architecture

### High-Level Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│   Customer ──▶ REST API ──▶ TicketService ──▶ DB (status: RECEIVED)      │
│                                     │                                    │
│                                     │ Publish: TicketCreatedEvent        │
│                                     │ @TransactionalEventListener        │
│                                     │ phase=AFTER_COMMIT                 │
│                                     ▼                                    │
│   ┌──────────────────────────────────────────────────────────────────┐   │
│   │                    ASYNC ROUTING (@Async)                        │   │
│   │                                                                  │   │
│   │   ┌─────────────────┐                                            │   │
│   │   │ Context Builder │  Load messages, RAG articles, patterns     │   │
│   │   └────────┬────────┘                                            │   │
│   │            │                                                     │   │
│   │            ▼                                                     │   │
│   │   ┌─────────────────┐     ┌─────────────────────────────────┐    │   │
│   │   │ Strategy        │────▶│ AgentRuntimeRoutingStrategy     │    │   │
│   │   │ Coordinator     │     │ (LangGraph4j State Machine)     │    │   │
│   │   └─────────────────┘     └─────────────────────────────────┘    │   │
│   │            │                         │                           │   │
│   │            │                         ▼                           │   │
│   │            │              ┌─────────────────────┐                │   │
│   │            │              │ PLAN → SAFETY →     │                │   │
│   │            │              │ TOOL_EXEC → REFLECT │                │   │
│   │            │              └─────────────────────┘                │   │
│   │            │                         │                           │   │
│   │            ▼                         ▼                           │   │
│   │   ┌─────────────────┐     ┌─────────────────────┐                │   │
│   │   │ Action Registry │────▶│ Execute: AUTO_REPLY,│                │   │
│   │   │ (ActionHandler) │     │ ASSIGN_QUEUE, etc.  │                │   │
│   │   └─────────────────┘     └─────────────────────┘                │   │
│   │                                                                  │   │
│   └──────────────────────────────────────────────────────────────────┘   │
│                                     │                                    │
│                                     ▼                                    │
│                     DB (status: ASSIGNED) + SSE Progress Update          │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Why Event-Driven?

```
WITHOUT EVENTS (Synchronous):          WITH EVENTS (Asynchronous):
─────────────────────────────          ─────────────────────────────

API Call                               API Call
    │                                      │
    ▼                                      ▼
Create Ticket                           Create Ticket
    │                                      │
    ▼                                      ▼
Route Ticket (5-15 sec)                 Return Response
    │                                      │
    ▼                                      │
Return Response                         Customer gets instant feedback
    │                                      
Customer waits 5-15 seconds              Background: Event triggers routing
```

**Key Implementation:**

```java

@TransactionalEventListener(phase = AFTER_COMMIT)
public void handleTicketCreated(TicketCreatedEvent event) {
    routerOrchestrator.routeTicket(event.getTicketId()); // @Async
}
```

---

## 4. State Machine (LangGraph4j)

### Graph Structure

```
                              START
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────────┐         │
│   │    PLAN     │      │   SAFETY    │      │   TOOL_EXEC     │         │
│   │             │      │             │      │                 │         │
│   │  • Call LLM │      │  • Apply    │      │  • Execute      │         │
│   │  • Validate │      │    policies │      │    action       │         │
│   │  • Repair   │      │  • Override │      │  • Update       │         │
│   │             │      │    if unsafe│      │    ticket       │         │
│   └─────────────┘      └─────────────┘      └────────┬────────┘         │
│         ▲                                            │                  │
│         │                                            ▼                  │
│         │                                    ┌─────────────────┐        │
│         │                                    │    REFLECT      │        │
│         │                                    │                 │        │
│         │                                    │  • Continue?    │        │
│         │                                    │  • Terminate?   │        │
│         │                                    └────────┬────────┘        │
│         │                                             │                 │
│         │                    ┌────────────────────────┴────────────┐    │
│         │                    │                                     │    │
│         │               Continue                              Terminate │
│         │                    │                                     │    │
│         └────────────────────┘                                     └────┼───┐
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                                                              │
                                                                              ▼
                                                                     ┌─────────────────┐
                                                                     │   TERMINATE     │
                                                                     │                 │
                                                                     │  • Finalize     │
                                                                     │  • Close SSE    │
                                                                     │  • Return       │
                                                                     └────────┬────────┘
                                                                              │
                                                                              ▼
                                                                             END
```

### Node Responsibilities

| Node          | Purpose                | Input            | Output                            |
|---------------|------------------------|------------------|-----------------------------------|
| **PLAN**      | LLM decides routing    | Enriched context | `RouterResponse` (planned)        |
| **SAFETY**    | Apply policy gates     | Planned response | Safe response (possibly modified) |
| **TOOL_EXEC** | Execute the action     | Safe response    | Execution result                  |
| **REFLECT**   | Continue or terminate? | Current state    | `TO_PLAN` or `TO_TERMINATE`       |
| **TERMINATE** | Finalize run           | Final state      | Complete                          |

### State Object

```java
class AgentGraphState {
    // Identifiers
    Long ticketId;
    Long runtimeRunId;
    RouterRequest routerRequest;
    Instant startedAt;

    // Planning State
    String plannerRawJson;              // Raw LLM response
    RouterResponse plannedResponse;     // Parsed decision
    boolean fallbackUsed;
    AgentValidationErrorCode errorCode;

    // Execution State
    AgentSafetyDecision safetyDecision;
    AgentToolExecutionResult toolExecutionResult;
    RouterResponse finalResponse;

    // Multi-Agent State
    AgentRole actorRole;                // Who is acting now
    AgentRole targetRole;               // Who to delegate to
    boolean handoff;
    String handoffReason;

    // Control State
    int stepCount;
    List<AgentStepDecision> decisions;
    AgentTerminationReason terminationReason;
}
```

### Safety Limits

```properties
# Maximum state machine iterations
agent.runtime.max-steps=4
# Maximum execution time
agent.runtime.max-runtime-ms=15000
```

---

## 5. Rule-Based System & Safety Rules

### Why Rules Are Needed

```
┌─────────────────────────────────────────────────────────────────┐
│  THE PROBLEM WITH RAW LLM OUTPUT                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  LLM Output:                                                    │
│  {                                                              │
│    "category": "BILLING",                                       │
│    "priority": "CRITICAL",                                      │
│    "queue": "GENERAL_Q",        ← Wrong. BILLING → BILLING_Q    │
│    "next_action": "AUTO_RESOLVE",                               │
│    "confidence": 0.45           ← Too low for AUTO_RESOLVE      │
│  }                                                              │
│                                                                 │
│  Issues:                                                        │
│  ❌ Low confidence (0.45) but action is AUTO_RESOLVE             │
│  ❌ CRITICAL priority shouldn't auto-resolve                     │
│  ❌ Category is BILLING but queue is GENERAL_Q                   │
│                                                                 │
│  Without policy engine: This would be executed                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Policy Engine Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  POLICY ENGINE FLOW                                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  LLM Output ─────▶ Policy Engine ─────▶ Safe Output             │
│                         │                                       │
│                         ▼                                       │
│         ┌───────────────────────────────────────┐               │
│         │  for (rule : policyRules) {           │               │
│         │      // Rules ordered by @Order       │               │
│         │      outcome = rule.apply(state);     │               │
│         │      if (outcome.changed()) {         │               │
│         │          state = outcome.state();     │               │
│         │      }                                │               │
│         │  }                                    │               │
│         └───────────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule Interface

```java
interface RouterPolicyRule {
    RoutingPolicyRuleCode code();

    PolicyRuleOutcome apply(PolicyEvaluationState state);
}

class PolicyRuleOutcome {
    boolean changed();

    PolicyEvaluationState state();

    static PolicyRuleOutcome unchanged(state);

    static PolicyRuleOutcome changed(state);
}
```

### Three Built-in Safety Rules

```
┌─────────────────────────────────────────────────────────────────┐
│  RULE 1: SecurityContentEscalationRule (Order = 1)              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Purpose: Ensure security issues are always escalated           │
│                                                                 │
│  Trigger Conditions:                                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Category = SECURITY                                    │    │
│  │  OR                                                     │    │
│  │  rationaleTags contains:                                │    │
│  │    • "ACCOUNT_ACCESS"                                   │    │
│  │    • "UNAUTHORIZED"                                     │    │
│  │    • "DATA_BREACH"                                      │    │
│  │    • "PHISHING"                                         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Overrides Applied:                                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  queue      → SECURITY_Q                                │    │
│  │  priority   → HIGH                                      │    │
│  │  nextAction → ESCALATE                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Why First? Security overrides everything else                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RULE 2: CriticalMinConfidenceRule (Order = 2)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Purpose: Require high confidence for CRITICAL tickets          │
│                                                                 │
│  Trigger Conditions:                                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Priority = CRITICAL                                    │    │
│  │  AND                                                    │    │
│  │  confidence < CRITICAL_MIN_CONF (default: 0.90)         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Overrides Applied:                                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  nextAction → HUMAN_REVIEW                              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Example:                                                       │
│  Input:  priority=CRITICAL, confidence=0.75                     │
│  Output: nextAction=HUMAN_REVIEW                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RULE 3: AutoRouteThresholdRule (Order = 3)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Purpose: Require minimum confidence for autonomous actions     │
│                                                                 │
│  Trigger Conditions:                                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  confidence < AUTO_ROUTE_THRESHOLD (default: 0.75)      │    │
│  │  AND                                                    │    │
│  │  No previous rule triggered                             │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Overrides Applied:                                             │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  nextAction → HUMAN_REVIEW                              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Example:                                                       │
│  Input:  confidence=0.60                                        │
│  Output: nextAction=HUMAN_REVIEW                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule Decision Tree

```
                    RouterResponse
                          │
                          ▼
        ┌─────────────────────────────────────┐
        │ Rule 1: SecurityContentEscalation   │
        │                                     │
        │ Category = SECURITY?                │
        │ OR dangerous tags?                  │
        └───────────────┬─────────────────────┘
                        │
             ┌──────────┴──────────┐
             │                     │
            YES                    NO
             │                     │
             ▼                     ▼
  ┌──────────────────────┐   ┌─────────────────────────┐
  │ Override:            │   │ Rule 2: CriticalMinConf │
  │ queue → SECURITY_Q   │   │                         │
  │ priority → HIGH      │   │ Priority = CRITICAL     │
  │ action → ESCALATE    │   │ AND conf < 0.90?        │
  │                      │   └───────────┬─────────────┘
  │ policyTriggered=true │               │
  └──────────────────────┘   ┌───────────┴───────────┐
                             │                       │
                            YES                     NO
                             │                       │
                             ▼                       ▼
                  ┌──────────────────┐   ┌─────────────────────┐
                  │ Override:        │   │ Rule 3: AutoRoute   │
                  │ action →         │   │                     │
                  │ HUMAN_REVIEW     │   │ conf < 0.75         │
                  │                  │   │ AND not triggered?  │
                  │ policyTriggered  │   └───────────┬─────────┘
                  │ = true           │               │
                  └──────────────────┘   ┌───────────┴───────────┐
                                         │                       │
                                        YES                     NO
                                         │                       │
                                         ▼                       ▼
                              ┌──────────────────┐    ┌────────────────┐
                              │ Override:        │    │ No changes     │
                              │ action →         │    │                │
                              │ HUMAN_REVIEW     │    │ Return as-is   │
                              └──────────────────┘    └────────────────┘
```

## 6. JSON Validation & Auto-Repair

### The Problem

```
┌─────────────────────────────────────────────────────────────────┐
│  LLM CAN RETURN INVALID JSON                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Expected:                                                      │
│  {                                                              │
│    "category": "BILLING",                                       │
│    "priority": "HIGH",                                          │
│    "queue": "BILLING_Q",                                        │
│    "nextAction": "AUTO_REPLY",                                  │
│    "confidence": 0.92                                           │
│  }                                                              │
│                                                                 │
│  What LLM might return:                                         │
│  {                                                              │
│    "category": "BILLING",                                       │
│    "priority": "HIGH",                                          │
│    "queue": "billing_queue",     ← Wrong format (should be enum)│
│    "next_action": "AUTO_REPLY",                                 │
│    "confidence": "high"          ← Wrong type (should be number)│
│  }                                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Validation Pipeline

```
┌─────────────────────────────────────────────────────────────────────────┐
│  JSON VALIDATION PIPELINE                                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Raw LLM Response                                                       │
│        │                                                                │
│        ▼                                                                │
│  ┌─────────────────┐                                                    │
│  │ Step 1: PARSE   │                                                    │
│  │                 │──── Parse as JSON ────▶ Success / Fail             │
│  └────────┬────────┘                                                    │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────┐                                                    │
│  │ Step 2: VALIDATE│                                                    │
│  │                 │──── Check schema ────▶ Valid / Invalid             │
│  └────────┬────────┘                                                    │
│           │                                                             │
│           │ IF INVALID                                                  │
│           ▼                                                             │
│  ┌─────────────────┐     ┌─────────────────┐                            │
│  │ Step 3: REPAIR  │────▶│ Retry Counter   │                            │
│  │ (if enabled)    │     │ < max retries?  │                            │
│  └────────┬────────┘     └────────┬────────┘                            │
│           │                       │                                     │
│           │ YES                   │ NO                                  │
│           ▼                       ▼                                     │
│  ┌─────────────────┐     ┌─────────────────┐                            │
│  │ Ask LLM to fix  │     │ FALLBACK to     │                            │
│  │ its own output  │     │ HUMAN_REVIEW    │                            │
│  └────────┬────────┘     └─────────────────┘                            │
│           │                                                             │
│           └──────▶ Back to Step 1                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# Enable schema validation
agent.runtime.schema-enforcement-enabled=true
# Enable auto-repair of invalid responses
agent.runtime.repair-enabled=true
# Number of repair attempts before fallback
agent.runtime.planner-validation-retries=1
```

### Validation Error Codes

| Code                      | Meaning                        |
|---------------------------|--------------------------------|
| `MISSING_REQUIRED_FIELD`  | Required field not in response |
| `INVALID_CATEGORY`        | Category not in allowed values |
| `INVALID_PRIORITY`        | Priority not in allowed values |
| `INVALID_QUEUE`           | Queue not in allowed values    |
| `INVALID_NEXT_ACTION`     | Action not in allowed values   |
| `CONFIDENCE_OUT_OF_RANGE` | Confidence not 0.0-1.0         |
| `SCHEMA_VIOLATION`        | JSON doesn't match schema      |
| `PARSE_ERROR`             | Couldn't parse JSON at all     |

### Auto-Repair Mechanism

```
┌─────────────────────────────────────────────────────────────────┐
│  AUTO-REPAIR EXAMPLE                                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Original LLM Response (Invalid):                               │
│  {                                                              │
│    "category": "BILLING",                                       │
│    "next_action": "auto_reply"  ← lowercase, wrong key          │
│  }                                                              │
│                                                                 │
│  Repair Prompt to LLM:                                          │
│  "The previous JSON had these errors:                           │
│   - 'next_action' should be 'nextAction'                        │
│   - 'auto_reply' should be 'AUTO_REPLY' (enum)                  │
│   - Missing required fields: priority, queue, confidence        │
│   Please fix and return valid JSON."                            │
│                                                                 │
│  Repaired Response:                                             │
│  {                                                              │
│    "category": "BILLING",                                       │
│    "priority": "MEDIUM",                                        │
│    "queue": "BILLING_Q",                                        │
│    "nextAction": "AUTO_REPLY",                                  │
│    "confidence": 0.85                                           │
│  }                                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Single vs Multi-Agent Orchestration

### Orchestration Modes

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ORCHESTRATION MODES                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  SINGLE_AGENT                        MULTI_AGENT                        │
│  ═════════════                       ═══════════                        │
│                                                                         │
│  Config:                             Config:                            │
│  orchestration-mode=SINGLE_AGENT     orchestration-mode=MULTI_AGENT     │
│                                                                         │
│  ┌───────────────────────┐           ┌───────────────────────────────┐  │
│  │      SUPERVISOR       │           │          SUPERVISOR           │  │
│  │                       │           │                               │  │
│  │  One LLM call         │           │  Step 1: Analyze & Delegate   │  │
│  │  handles everything:  │           │          │                    │  │
│  │                       │           │          │ targetRole=RESOLVER│  │
│  │  • Analyze            │           │          │ handoff=true       │  │
│  │  • Categorize         │           │          ▼                    │  │
│  │  • Prioritize         │           │  ┌───────────────────────┐    │  │
│  │  • Generate response  │           │  │      RESOLVER         │    │  │
│  │  • All in one call    │           │  │      (Specialist)     │    │  │
│  │                       │           │  │                       │    │  │
│  └───────────────────────┘           │  │  Step 2: Generate     │    │  │
│                                      │  │  specialist response  │    │  │
│  LLM Calls: 1                        │  │                       │    │  │
│  Latency: Lower                      │  └───────────────────────┘    │  │
│  Cost: Lower                         │                               │  │
│                                      │  LLM Calls: 2                 │  │
│                                      │  Latency: Higher              │  │
│                                      │  Cost: Higher                 │  │
│                                      │  Quality: Potentially better  │  │
│                                      └───────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Agent Roles

```
┌─────────────────────────────────────────────────────────────────┐
│  AGENT ROLE HIERARCHY                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                     ┌─────────────────┐                         │
│                     │   SUPERVISOR    │                         │
│                     │  (Orchestrator) │                         │
│                     │                 │                         │
│                     │  • First call   │                         │
│                     │  • Delegates    │                         │
│                     │  • Coordinates  │                         │
│                     └────────┬────────┘                         │
│                              │                                  │
│           ┌──────────────────┼──────────────────┐               │
│           │                  │                  │               │
│           ▼                  ▼                  ▼               │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐    │
│  │   CLASSIFIER    │ │    RESOLVER     │ │       QA        │    │
│  │   (Specialist)  │ │   (Specialist)  │ │   (Specialist)  │    │
│  ├─────────────────┤ ├─────────────────┤ ├─────────────────┤    │
│  │ ASSIGN_QUEUE    │ │ AUTO_REPLY      │ │ ASK_CLARIFYING  │    │
│  │ CHANGE_PRIORITY │ │ USE_KNOWLEDGE   │ │                 │    │
│  │ ADD_INTERNAL_   │ │ USE_TEMPLATE    │ │                 │    │
│  │   NOTE          │ │ UPDATE_CUSTOMER │ │                 │    │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘    │
│                                                                 │
│                     ┌─────────────────┐                         │
│                     │   ESCALATOR     │                         │
│                     │   (Specialist)  │                         │
│                     ├─────────────────┤                         │
│                     │ HUMAN_REVIEW    │                         │
│                     │ ESCALATE        │                         │
│                     │ AUTO_ESCALATE   │                         │
│                     └─────────────────┘                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Action-to-Role Mapping

```
┌─────────────────────────────────────────────────────────────────┐
│  ACTION → ROLE MAPPING                                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  NextAction                 Target Role         Category        │
│  ════════════════════════════════════════════════════════════   │
│                                                                 │
│  ESCALATOR:                                                     │
│  ├── HUMAN_REVIEW          → Human review required              │
│  ├── ESCALATE              → Supervisor escalation              │
│  └── AUTO_ESCALATE         → System-triggered escalation        │
│                                                                 │
│  QA:                                                            │
│  └── ASK_CLARIFYING        → Need more information              │
│                                                                 │
│  RESOLVER:                                                      │
│  ├── AUTO_REPLY            → Generate response                  │
│  ├── AUTO_RESOLVE          → Close with resolution              │
│  ├── USE_KNOWLEDGE_ARTICLE → Apply KB solution                  │
│  ├── USE_TEMPLATE          → Apply response template            │
│  └── UPDATE_CUSTOMER_PROFILE→ Update customer data              │
│                                                                 │
│  CLASSIFIER:                                                    │
│  ├── ASSIGN_QUEUE          → Route to team                      │
│  ├── CHANGE_PRIORITY       → Adjust urgency                     │
│  ├── ADD_INTERNAL_NOTE     → Document context                   │
│  ├── TRIGGER_NOTIFICATION  → Alert stakeholders                 │
│  └── REOPEN_TICKET         → Reopen closed issue                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Delegation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  MULTI-AGENT DELEGATION FLOW                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: SUPERVISOR makes initial decision                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  LLM Response includes:                                 │    │
│  │  {                                                      │    │
│  │    "next_action": "AUTO_REPLY",                         │    │
│  │    "category": "BILLING",                               │    │
│  │    "draft_reply": null  ← May be null initially         │    │
│  │  }                                                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          │                                      │
│                          ▼                                      │
│  Step 2: Determine target role                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  switch (nextAction) {                                  │    │
│  │    ASK_CLARIFYING  → QA                                 │    │
│  │    HUMAN_REVIEW    → ESCALATOR                          │    │
│  │    ESCALATE        → ESCALATOR                          │    │
│  │    AUTO_REPLY      → RESOLVER                          │    │
│  │    ASSIGN_QUEUE    → CLASSIFIER                         │    │
│  │    ...                                                  │    │
│  │  }                                                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          │                                      │
│                          ▼                                      │
│  Step 3: Make second LLM call with specialist prompt            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  if (mode == MULTI_AGENT && targetRole != SUPERVISOR) { │    │
│  │      specialistResponse = llm.decideForRole(            │    │
│  │          request, ticketId, targetRole                  │    │
│  │      );                                                 │    │
│  │      state.setHandoff(true);                            │    │
│  │      state.setTargetRole(targetRole);                   │    │
│  │  }                                                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### When to Use Each Mode

```
┌─────────────────────────────────────────────────────────────────┐
│  MODE SELECTION GUIDE                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Use SINGLE_AGENT when:                                         │
│  ────────────────────────                                       │
│  ✓ Speed is critical (real-time responses)                      │
│  ✓ Cost is a concern (fewer LLM calls)                          │
│  ✓ Tasks are relatively simple                                  │
│  ✓ Using a capable model                                        │
│  ✓ Initial rollout / testing phase                              │
│                                                                 │
│  Use MULTI_AGENT when:                                          │
│  ────────────────────────                                       │
│  ✓ Quality is more important than speed                         │
│  ✓ Tasks benefit from specialization                            │
│  ✓ Complex reasoning required                                   │
│  ✓ Content generation is important (drafts, questions)          │
│  ✓ You want role-specific prompts                               │
│                                                                 │
│  Example Scenarios:                                             │
│  ──────────────────                                             │
│                                                                 │
│  "How do I change my password?"                                 │
│  → Action: ASSIGN_QUEUE                                         │
│  → Recommendation: SINGLE_AGENT (simple categorization)         │
│                                                                 │
│  "My internet is slow, I've tried restarting..."                │
│  → Action: AUTO_REPLY (needs quality response)                  │
│  → Recommendation: MULTI_AGENT (RESOLVER generates better)      │
│                                                                 │
│  "It's not working"                                             │
│  → Action: ASK_CLARIFYING                                       │
│  → Recommendation: MULTI_AGENT (QA specialist trained for this) │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Resilience & Key Takeaways

### Three Pillars of Resilience

```
┌─────────────────────────────────────────────────────────────────────────┐
│  RESILIENCE ARCHITECTURE                                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. CIRCUIT BREAKER                                                     │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                                                                 │    │
│  │     ┌─────────┐        ┌──────────┐        ┌───────────┐        │    │
│  │     │ CLOSED  │───────▶│  OPEN    │───────▶│ HALF_OPEN │        │    │
│  │     │ (normal)│        │(failfast)│        │ (testing) │        │    │
│  │     └────▲────┘        └──────────┘        └─────┬─────┘        │    │
│  │          │                   ▲                   │              │    │
│  │          │                   │                   │              │    │
│  │          └───────────────────┴───────────────────┘              │    │
│  │                                                                 │    │
│  │  Config: 50% failure rate → open, 30s wait before retry         │    │
│  │                                                                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  2. RETRY WITH EXPONENTIAL BACKOFF                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                                                                 │    │
│  │  Attempt 1 ──▶ FAIL, wait 1s                                    │    │
│  │  Attempt 2 ──▶ FAIL, wait 2s                                    │    │
│  │  Attempt 3 ──▶ SUCCESS or FALLBACK                              │    │
│  │                                                                 │    │
│  │  Why backoff? Gives LLM service time to recover                 │    │
│  │                                                                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  3. FALLBACK TO HUMAN REVIEW                                            │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                                                                 │    │
│  │  When triggered:                                                │    │
│  │  • Circuit breaker is OPEN                                      │    │
│  │  • All retries exhausted                                        │    │
│  │  • Timeout exceeded                                             │    │
│  │  • Schema validation failed (after repair attempts)             │    │
│  │                                                                 │    │
│  │  Fallback Response:                                             │    │
│  │  return RouterResponse.builder()                                │    │
│  │      .nextAction(HUMAN_REVIEW)  // Safest option                │    │
│  │      .confidence(BigDecimal.ZERO)                               │    │
│  │      .build();                                                  │    │
│  │                                                                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.llmCircuit.sliding-window-size=10
resilience4j.circuitbreaker.instances.llmCircuit.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.llmCircuit.wait-duration-in-open-state=30s
# Retry
resilience4j.retry.instances.llmRetry.max-attempts=3
resilience4j.retry.instances.llmRetry.wait-duration=1s
resilience4j.retry.instances.llmRetry.enable-exponential-backoff=true
resilience4j.retry.instances.llmRetry.exponential-backoff-multiplier=2
# Timeout
resilience4j.timelimiter.instances.llmTimeout.timeout-duration=30s
```

---

## Key Takeaways

### 1. Agentic ≠ Traditional LLM

```
Traditional:  Input ──▶ LLM ──▶ Output

Agentic:      Input ──▶ PLAN ──▶ SAFETY ──▶ EXECUTE ──▶ REFLECT
                              │
                              └── Policy gates prevent bad decisions
```

### 2. Rule-Based Safety System

```
┌─────────────────────────────────────────────────────────────────┐
│  THREE LAYERS OF PROTECTION                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: Schema Validation                                     │
│           Ensures LLM output is structurally valid              │
│                                                                 │
│  Layer 2: Auto-Repair                                           │
│           Attempts to fix invalid output before giving up       │
│                                                                 │
│  Layer 3: Policy Rules                                          │
│           Enforces business rules regardless of LLM decision    │
│           - Security issues always escalated                    │
│           - Low confidence → human review                       │
│           - Critical priority → requires high confidence        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3. Graceful Degradation

```
LLM fails?      ──▶ Circuit breaker opens  ──▶ Fallback to human
Low confidence? ──▶ Policy rule triggers   ──▶ Human review
Schema invalid? ──▶ Auto-repair attempt    ──▶ Then fallback
Max steps?      ──▶ TERMINATE              ──▶ Return what we have
```

### 4. Full Traceability

```
Every decision recorded in:
├── agent_runtime_run    (overall execution)
└── agent_runtime_step   (each node execution)

Queryable:
• Why did this route to TECH_Q?
• What was the confidence?
• Which policy rule triggered?
• What was the raw LLM output?
```

### 5. Flexible Orchestration

```
┌─────────────────────────────────────────────────────────────────┐
│  Operating Modes                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Production:     enabled=true, shadow-mode=false                │
│                  Full execution with actions                    │
│                                                                 │
│  Testing:        enabled=true, shadow-mode=true                 │
│                  Execute state machine, don't apply actions     │
│                                                                 │
│  Classic:        enabled=false                                  │
│                  Single LLM call, no state machine              │
│                                                                 │
│  Canary:         canary-enabled=true, allowed-queues=...        │
│                  Gradual rollout to specific queues             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Questions to Expect

| Question                              | Answer                                                                                            |
|---------------------------------------|---------------------------------------------------------------------------------------------------|
| Why `AFTER_COMMIT` for events?        | Ticket must exist in DB before routing starts; prevents "ghost" routing if transaction rolls back |
| What if LLM returns invalid JSON?     | Auto-repair with retry (configurable), then fallback to HUMAN_REVIEW                              |
| How do you prevent runaway actions?   | `max-steps`, `max-runtime-ms`, action budgets in policy config                                    |
| What's the safe default?              | HUMAN_REVIEW - always safe, human makes the decision                                              |
| How does multi-agent delegation work? | Supervisor analyzes, delegates to specialist based on action type                                 |
| Why use circuit breaker?              | Prevents thread exhaustion on LLM failure; instant fallback when LLM is down                      |
| Can I add custom rules?               | Yes - implement `RouterPolicyRule`, add `@Order`, register in enum                                |

---

*For full documentation, see `docs/agentic-system/` directory*
