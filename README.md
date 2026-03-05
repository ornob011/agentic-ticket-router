# Agentic Ticket Router

An AI-powered support ticket routing system that uses autonomous multi-step reasoning to classify, prioritize, and route
customer support tickets. Built with a LangGraph4j state machine, policy-based guardrails, and full decision
traceability.

Unlike traditional LLM wrappers that make a single classification call, this system operates as an **agentic loop** — it
plans actions, validates them against safety policies, executes tools, and reflects on outcomes before finalizing a
decision.

---

## Table of Contents

- [Why This Is Agentic (Not Just an LLM Wrapper)](#why-this-is-agentic-not-just-an-llm-wrapper)
- [Achieving Reliability with a 4B Parameter Model](#achieving-reliability-with-a-4b-parameter-model)
- [Key Capabilities](#key-capabilities)
- [Guardrails & Autonomous Action Restrictions](#guardrails--autonomous-action-restrictions)
- [Human Review](#human-review)
- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Agent Runtime](#agent-runtime)
- [Policy Engine](#policy-engine)
- [Resilience](#resilience)
- [Frontend](#frontend)
- [Default Users](#default-users)
- [Documentation](#documentation)

---

## Why This Is Agentic (Not Just an LLM Wrapper)

Most "AI-powered" systems make a single LLM call and return the result. This system is fundamentally different — it runs
a **multi-step reasoning loop** where the AI plans, validates, acts, and reflects before committing to a decision.

### Single-Shot LLM vs. Agentic Loop

| Aspect              | Traditional LLM Wrapper         | This System                                                |
|---------------------|---------------------------------|------------------------------------------------------------|
| **Reasoning**       | Single prompt in, response out  | Multi-step state machine (PLAN → SAFETY → ACT → REFLECT)   |
| **Memory**          | None between calls              | State persists across steps within a run                   |
| **Output Handling** | Trust raw LLM output            | JSON Schema validation + auto-repair loop                  |
| **Guardrails**      | None                            | Policy engine with ordered safety rules                    |
| **Actions**         | Returns text                    | Executes real actions (assign queue, send reply, escalate) |
| **Self-Correction** | None                            | Reflects on outcomes, loops back to re-plan if needed      |
| **Failure Mode**    | Silent failure or hallucination | Graceful fallback to human review with full trace          |
| **Observability**   | Logs only                       | Every step persisted with inputs, outputs, and timing      |
| **Agents**          | Single                          | Supervisor → Specialist delegation (multi-agent)           |

### The Reasoning Loop

Each ticket goes through a **Plan-Safety-Act-Reflect** cycle implemented as a LangGraph4j state machine:

```
   ┌────────┐     ┌────────┐     ┌────────────┐     ┌─────────┐
   │  PLAN  │────>│ SAFETY │────>│ TOOL_EXEC  │────>│ REFLECT │──┐
   └────────┘     └────────┘     └────────────┘     └─────────┘  │
       ^                                                         │
       └──────────────── (loop if needed) ───────────────────────┘
                                                      │
                                                ┌─────▼─────┐
                                                │ TERMINATE │
                                                └───────────┘
```

1. **PLAN** — The LLM analyzes the ticket and decides: category, priority, queue, confidence, and next action. Output is
   validated against a JSON Schema and auto-repaired if malformed.
2. **SAFETY** — The planned action is evaluated against policy rules. Security content triggers forced escalation.
   Low-confidence decisions are blocked and rerouted to human review.
3. **TOOL_EXEC** — The approved action executes: assign to queue, send auto-reply, ask a clarifying question, escalate,
   search the knowledge base, or update customer profile.
4. **REFLECT** — The system checks: Was the goal reached? Is the budget exhausted? Did safety block the action? Has the
   max step count been hit? If not resolved, loop back to PLAN.
5. **TERMINATE** — Persists the final routing decision, completes SSE streaming, and records the full agent run trace.

This is not a single LLM call with post-processing — it is an autonomous loop that can take multiple actions, correct
itself, and defer to humans when it cannot proceed safely.

---

## Achieving Reliability with a 4B Parameter Model

This system runs on **qwen3:4b-instruct** — a 4 billion parameter model running locally on consumer hardware via
Ollama. No GPT-4, no 70B model, no cloud API required. Reliable structured output comes from **engineering, not model
scale**.

### How It Works

| Technique                          | What It Does                                                                                                        |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| **Temperature 0.0**                | Deterministic output — same input always produces the same response                                                 |
| **Forced JSON format**             | Ollama's `format=json` parameter forces the model to output valid JSON                                              |
| **Strict enum injection**          | Prompts include the exact list of valid values for every enum field — the model picks from a closed set             |
| **JSON Schema 2020-12 validation** | Every LLM response is validated against a strict JSON Schema before any action is taken                             |
| **Auto-repair loop**               | If validation fails, the error message and original JSON are sent back to the model with a repair prompt            |
| **Contract validation**            | Post-parse checks: field presence, value ranges, action-specific contracts (e.g., AUTO_REPLY requires a reply body) |
| **Safe fallback**                  | If repair is exhausted → route to `GENERAL_Q` with `HUMAN_REVIEW` and confidence `0.0`                              |

### The Validation Pipeline

```
          LLM Response
               │
               ▼

┌─────────────────────┐     ┌──────────────────┐     ┌────────────────────┐
│  JSON Schema Check  │──X──│  Repair Prompt   │──X──│  Fallback:         │
│  (Draft 2020-12)    │     │  (send error +   │     │  GENERAL_Q +       │
│                     │     │   original JSON  │     │  HUMAN_REVIEW +    │
│  Valid? ────────────│──>  │   back to model) │     │  confidence = 0.0  │
│         continue    │     │                  │     │                    │
│                     │     │  Valid? ─────────│────>│                    │
│                     │     │         continue │     │                    │
└─────────────────────┘     └──────────────────┘     └────────────────────┘
```

### Observability

Every parse attempt is tracked with a `ParseStatus`:

| Status             | Meaning                                      |
|--------------------|----------------------------------------------|
| `SUCCESS`          | Parsed and validated on first attempt        |
| `REPAIR_ATTEMPTED` | Validation failed, repair prompt sent        |
| `REPAIR_SUCCESS`   | Repaired successfully after retry            |
| `REPAIR_FAILED`    | Repair exhausted — fell back to safe default |
| `INVALID_JSON`     | Model returned non-JSON output               |
| `SCHEMA_VIOLATION` | Valid JSON but violates schema constraints   |
| `MODEL_ERROR`      | Model returned an error response             |
| `TIMEOUT`          | Model inference timed out                    |

The result: a 4B model produces reliable, validated, structured routing decisions — because the system never trusts raw
LLM output.

---

## Key Capabilities

| Capability                    | Description                                                                                  |
|-------------------------------|----------------------------------------------------------------------------------------------|
| **Autonomous Routing**        | Classifies tickets by category, priority, and queue without human intervention               |
| **Auto-Reply**                | Generates and sends responses for straightforward issues using knowledge base articles       |
| **Clarifying Questions**      | Detects ambiguous tickets and asks targeted follow-up questions (budgeted, max 3)            |
| **Security Escalation**       | Automatically flags security-related tickets, overrides queue to `SECURITY_Q`, and escalates |
| **Draft Reply Streaming**     | Streams AI-generated reply drafts to agents via SSE for review before sending                |
| **Pattern Learning**          | Learns from successful routing outcomes to improve future confidence scores                  |
| **Human-in-the-Loop**         | Falls back to human review when confidence is below threshold or safety constraints trigger  |
| **Multi-Agent Orchestration** | Supports supervisor-specialist delegation via configurable orchestration modes               |
| **Full Observability**        | Every agent step (plan, safety check, tool execution, reflection) is persisted and traceable |

---

## Guardrails & Autonomous Action Restrictions

The system is designed to **restrict fully autonomous action** through layered safety mechanisms. No single point of
failure can cause an unchecked AI decision to reach a customer.

### Confidence Thresholds

| Threshold              | Value  | Effect                                                                                 |
|------------------------|--------|----------------------------------------------------------------------------------------|
| `AUTO_ROUTE_THRESHOLD` | `0.70` | Below this, the system cannot auto-route — forced `HUMAN_REVIEW`                       |
| `CRITICAL_MIN_CONF`    | `0.85` | CRITICAL priority tickets require this confidence or higher — otherwise `HUMAN_REVIEW` |

Both thresholds are configurable via the `policy_config` database table and the admin API.

### Action Budgets

| Budget                     | Limit | Effect                                                                        |
|----------------------------|-------|-------------------------------------------------------------------------------|
| `MAX_AUTONOMOUS_ACTIONS`   | `5`   | After 5 autonomous actions on a single ticket, the system stops and escalates |
| `MAX_QUESTIONS_PER_TICKET` | `3`   | Maximum clarifying questions before escalating to a human                     |

### Runtime Limits

| Limit            | Value   | Effect                                      |
|------------------|---------|---------------------------------------------|
| `max-steps`      | `4`     | Maximum state machine iterations per ticket |
| `max-runtime-ms` | `15000` | 15-second timeout per agent execution       |

### Frustration & Loop Detection

The system monitors for patterns that indicate a customer is going in circles:

- **Loop detection**: If 3+ recent questions exist but only 1 or fewer are distinct, the system detects circular
  questioning and auto-escalates to a human agent
- **Frustration detection**: If the customer shows signs of frustration, the system immediately escalates rather than
  continuing autonomous interaction

Both are independently toggleable via policy flags (`FRUSTRATION_DETECTION_ENABLED`, `LOOP_DETECTION_ENABLED`).

### Security Escalation

When the system detects security-related content — either via `SECURITY` category classification or the presence of
security tags in the rationale — it triggers a forced override:

- Queue → `SECURITY_Q`
- Priority → `HIGH`
- Action → `ESCALATE`

Security tags that trigger escalation: `THREAT`, `PII_RISK`, `SECURITY_BREACH`, `HACK`, `BREACH`, `VULNERABILITY`,
`MALWARE`, `CYBER_ATTACK`, `EXPLOIT`.

### Policy Engine Ordering

Rules execute in strict priority order. Earlier rules' overrides are respected by later rules:

| Priority | Rule                            | Trigger                                      |
|----------|---------------------------------|----------------------------------------------|
| 1st      | `SecurityContentEscalationRule` | `SECURITY` category or security tags         |
| 2nd      | `CriticalMinConfidenceRule`     | `CRITICAL` priority + confidence < 0.85      |
| 3rd      | `AutoRouteThresholdRule`        | Confidence < 0.70 (if no earlier rule fired) |

### Fallback on Failure

When the LLM circuit breaker is open or all retries are exhausted, the system does not hang or crash — it returns a
**safe fallback routing decision**: route to `GENERAL_Q` with `HUMAN_REVIEW` action.

---

## Human Review

The system is designed to **defer to humans** whenever it cannot act with sufficient confidence or safety. Human review
is not a failure mode — it is an intentional design constraint.

### When the System Defers

| Trigger                                | Mechanism                                                                         |
|----------------------------------------|-----------------------------------------------------------------------------------|
| **Low confidence**                     | Confidence < 0.70 → `AutoRouteThresholdRule` forces `HUMAN_REVIEW`                |
| **Critical + insufficient confidence** | CRITICAL priority + confidence < 0.85 → `CriticalMinConfidenceRule` forces review |
| **Security content**                   | SECURITY category or security tags → forced `ESCALATE` to `SECURITY_Q`            |
| **Action budget exhausted**            | 5+ autonomous actions on one ticket → escalation                                  |
| **Question budget exhausted**          | 3+ clarifying questions asked → escalation                                        |
| **Loop detection**                     | 3+ recent questions with ≤1 distinct → auto-escalation                            |
| **Frustration detection**              | Customer frustration signals → immediate escalation                               |
| **JSON repair failure**                | LLM output cannot be repaired → fallback to `GENERAL_Q` + `HUMAN_REVIEW`          |
| **Circuit breaker open**               | LLM unavailable → safe fallback with `HUMAN_REVIEW`                               |
| **Max steps / timeout**                | Agent exceeds 4 steps or 15s → terminates with current best decision              |
| **Agent override**                     | Human agents can always override any AI routing decision via the UI               |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
│   React 18 SPA (Vite + Tailwind)      REST API (/api/v1/*)          │
│   Role-based dashboards                SSE streaming endpoints      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         ROUTING LAYER                               │
│   RouterOrchestrator (@Async)  →  RoutingExecutionCoordinator       │
│                                          │                          │
│                              ┌───────────┴───────────┐              │
│                              ▼                       ▼              │
│                   Agent Runtime Strategy    Classic Routing Strategy│
│                   (LangGraph4j state       (Single LLM call +       │
│                    machine, multi-step)     policy engine)          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         AGENTIC LAYER                               │
│                                                                     │
│   ┌────────┐   ┌────────┐   ┌────────────┐   ┌─────────┐            │
│   │  PLAN  │──>│ SAFETY │──>│ TOOL_EXEC  │──>│ REFLECT │──┐         │
│   └────────┘   └────────┘   └────────────┘   └─────────┘  │         │
│       ^                                                   │         │
│       └──────────────── (loop if needed) ─────────────────┘         │
│                                                    │                │
│                                              ┌─────▼─────┐          │
│                                              │ TERMINATE │          │
│                                              └───────────┘          │
│                                                                     │
│   Policy Engine  ·  Safety Evaluator  ·  Schema Validator           │
│   Tool Executor  ·  Fallback Service  ·  Trace Service              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          AI / ML LAYER                              │
│   Spring AI 1.1.2  ·  Ollama / OpenAI  ·  pgvector (RAG)            │
│   Prompt Templates (.st)  ·  JSON Schema Validation + Auto-Repair   │
│   Token Counting (jtokkit)  ·  ICU4J Keyword Extraction             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       INFRASTRUCTURE LAYER                          │
│   PostgreSQL 16 + JSONB + pgvector  ·  HikariCP Connection Pool     │
│   Resilience4j (Circuit Breaker + Retry)  ·  Caffeine Cache         │
│   SSE Engine (Real-time)  ·  Spring Events (Async)                  │
│   JDBC Chat Memory  ·  Structured Logging                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer                   | Technologies                                                                  |
|-------------------------|-------------------------------------------------------------------------------|
| **Backend**             | Java 21, Spring Boot 3.5.9, Spring Data JPA, Spring Security, Spring AI 1.1.2 |
| **Agent Orchestration** | LangGraph4j 1.5.10 (state machine), multi-agent support                       |
| **LLM**                 | Ollama (`qwen3:4b-instruct` for chat, `nomic-embed-text` for embeddings)      |
| **Database**            | PostgreSQL 16 + pgvector extension (JSONB, vector similarity search)          |
| **Resilience**          | Resilience4j (circuit breaker, retry with exponential backoff, time limiter)  |
| **Frontend**            | React 18, TypeScript 5.7, Vite 6, Tailwind CSS 4, Radix UI, TanStack Query    |
| **Build**               | Maven (with frontend-maven-plugin for integrated React builds)                |
| **Caching**             | Caffeine (in-memory, 5-minute TTL)                                            |
| **NLP**                 | ICU4J 77.1 (Unicode word tokenization), jtokkit (token counting)              |

---

## Prerequisites

- **Java 21** JDK
- **Docker & Docker Compose** (for PostgreSQL + pgvector)
- **Ollama** (local LLM inference)
- **Node.js v22+** (auto-installed by Maven, or manual for frontend dev)

---

## Getting Started

### 1. Start the Database

```bash
docker compose up -d
```

This starts PostgreSQL 16 with pgvector on `localhost:5432`.

### 2. Initialize the Schema

Execute the DDL and seed data scripts in order against the database (`dsi_agentic_ticketing` / `agentic_ticketing_app` /
`dev_strong_password_change_later`):

```bash
# Schema
psql -h localhost -U agentic_ticketing_app -d dsi_agentic_ticketing -f sql/ddl/ddl.sql

# Seed data (execute in order)
for f in sql/dml/V00{1..9}*.sql sql/dml/V01{0..2}*.sql; do
  psql -h localhost -U agentic_ticketing_app -d dsi_agentic_ticketing -f "$f"
done
```

Or open each file in IntelliJ IDEA and execute against your database connection.

### 3. Set Up Ollama

```bash
ollama pull qwen3:4b-instruct
ollama pull nomic-embed-text
ollama serve
```

### 4. Vector Store Embeddings (Choose One)

**Option A — Pre-computed (faster startup):**

1.

Download [V013__seed_vector_store.sql](https://drive.google.com/file/d/1_E5a4Sc-mbDUgGMdeAC-F7bXnBlIIiu4/view?usp=drive_link) (~
300 MB)

2. Execute it against the database
3. Execute `sql/dml/V013__set_vector_store_initialized.sql`

**Option B — Generate at startup:**
Skip the download. The application generates embeddings on first run automatically.

### 5. Build and Run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Access the application at **http://localhost:8080**

---

## Configuration

### Core Settings (`application.properties`)

| Property                                    | Default             | Description                                  |
|---------------------------------------------|---------------------|----------------------------------------------|
| `spring.ai.model.chat`                      | `ollama`            | LLM provider (`ollama` or `openai`)          |
| `spring.ai.ollama.chat.options.model`       | `qwen3:4b-instruct` | Chat model                                   |
| `spring.ai.ollama.chat.options.temperature` | `0.0`               | Generation temperature                       |
| `spring.ai.ollama.chat.options.num-ctx`     | `16384`             | Context window size                          |
| `agent.runtime.orchestration-mode`          | `SINGLE_AGENT`      | `SINGLE_AGENT` or `MULTI_AGENT_HIERARCHICAL` |
| `agent.runtime.max-steps`                   | `4`                 | Max state machine iterations per ticket      |
| `agent.runtime.max-runtime-ms`              | `15000`             | Execution timeout per ticket                 |
| `agent.runtime.repair-enabled`              | `true`              | Auto-repair malformed LLM JSON               |
| `agent.runtime.shadow-mode`                 | `false`             | Run agent without persisting decisions       |
| `agent.runtime.schema-enforcement-enabled`  | `true`              | Validate LLM output against JSON schema      |

### Resilience Settings

| Property                                                                       | Default | Description                      |
|--------------------------------------------------------------------------------|---------|----------------------------------|
| `resilience4j.circuitbreaker.instances.llmCircuit.failure-rate-threshold`      | `50`    | Open circuit at 50% failure rate |
| `resilience4j.circuitbreaker.instances.llmCircuit.wait-duration-in-open-state` | `30s`   | Cooldown before half-open        |
| `resilience4j.retry.instances.llmRetry.max-attempts`                           | `3`     | Max retry attempts               |
| `resilience4j.timelimiter.instances.llmCircuit.timeout-duration`               | `600s`  | LLM call timeout                 |

### Chat Memory Settings

| Property                                           | Default | Description                      |
|----------------------------------------------------|---------|----------------------------------|
| `ticket.chat-memory.enabled`                       | `true`  | Enable conversation memory       |
| `ticket.chat-memory.ticket-history-max-messages`   | `20`    | Messages per ticket context      |
| `ticket.chat-memory.customer-context-max-messages` | `10`    | Messages for customer enrichment |

### Profiles

- **Default**: Ollama-based, local development
- **`dev-ornob`**: Uses OpenAI-compatible API (Z.AI), tuned ingestion parameters

---

## Project Structure

```
agentic-ticket-router/
├── src/main/java/com/dsi/support/agenticrouter/
│   ├── configuration/          # Spring, security, async, cache, LLM configs
│   ├── controller/api/         # REST API controllers
│   ├── dto/                    # Request/response DTOs
│   ├── entity/                 # JPA entities (BaseEntity, SupportTicket, etc.)
│   ├── enums/                  # Domain enums (41 types)
│   ├── repository/             # Spring Data JPA repositories
│   ├── service/
│   │   ├── routing/            # Core routing: orchestrator, policy engine, strategies
│   │   ├── agentruntime/       # LangGraph4j agent: planner, safety, tooling, trace
│   │   │   ├── orchestration/  # State graph builder, state navigator, termination
│   │   │   ├── planner/        # LLM planner client, schema validation, fallback
│   │   │   ├── safety/         # Safety evaluator, decision model
│   │   │   ├── tooling/        # Tool executor, action adapter
│   │   │   ├── trace/          # Run/step persistence
│   │   │   └── streaming/      # SSE plugins for routing progress and draft replies
│   │   ├── action/             # Action handlers (auto-reply, assign, escalate, etc.)
│   │   ├── ai/                 # LLM client, prompt service, token counting
│   │   ├── knowledge/          # Vector store, RAG, knowledge ingestion
│   │   ├── learning/           # Pattern learning, keyword extraction
│   │   ├── policy/             # Policy evaluation and enforcement
│   │   ├── ticket/             # Ticket CRUD, lifecycle, status transitions
│   │   ├── dashboard/          # Role-based dashboard composition
│   │   ├── memory/             # Chat memory context enrichment
│   │   ├── sse/                # SSE engine, emitter registry, channel plugins
│   │   ├── auth/               # Authentication, profile, user management
│   │   ├── audit/              # Audit event logging
│   │   └── notification/       # Notification service
│   ├── security/               # Security filters, auth providers
│   ├── validator/              # Input validators
│   └── util/                   # Utilities (logging, pagination, normalization)
├── src/main/resources/
│   ├── prompts/                # 13 StringTemplate prompt files (.st)
│   ├── application.properties  # Main configuration
│   ├── messages_en.properties  # English i18n
│   └── messages_bn.properties  # Bengali i18n
├── frontend/                   # React SPA source (Vite + Tailwind)
├── sql/
│   ├── ddl/ddl.sql             # Full PostgreSQL schema
│   └── dml/                    # 15 seed migration files (V001–V013)
├── docs/
│   ├── agentic-system/         # 12-part technical documentation
│   ├── SETUP_GUIDE.md          # Quick start guide
│   └── AGENTIC-TEST-PROMPTS.md # Test prompts for validation
├── docker-compose.yml          # PostgreSQL + pgvector
└── pom.xml                     # Maven build (Java 21, Spring Boot 3.5.9)
```

---

## API Reference

### Tickets (`/api/v1/tickets`)

| Method  | Endpoint                       | Description                                             |
|---------|--------------------------------|---------------------------------------------------------|
| `GET`   | `/`                            | List tickets (scoped: `MINE`, `QUEUE`, `REVIEW`, `ALL`) |
| `POST`  | `/`                            | Create ticket (triggers async routing)                  |
| `GET`   | `/{ticketId}`                  | Get ticket detail                                       |
| `POST`  | `/{ticketId}/replies`          | Add message (customer or agent)                         |
| `PATCH` | `/{ticketId}/status`           | Change ticket status                                    |
| `PATCH` | `/{ticketId}/assign-agent`     | Assign agent to ticket                                  |
| `PATCH` | `/{ticketId}/assign-self`      | Agent self-assignment                                   |
| `PATCH` | `/{ticketId}/release-agent`    | Release agent assignment                                |
| `PATCH` | `/{ticketId}/override-routing` | Override AI routing decision                            |
| `GET`   | `/{ticketId}/routing/stream`   | SSE: real-time routing progress                         |
| `GET`   | `/{ticketId}/draft/stream`     | SSE: streamed draft reply generation                    |

### Auth (`/api/v1/auth`)

| Method | Endpoint           | Description                  |
|--------|--------------------|------------------------------|
| `POST` | `/login`           | Authenticate (session-based) |
| `POST` | `/logout`          | End session                  |
| `GET`  | `/me`              | Current user info            |
| `GET`  | `/profile`         | User profile                 |
| `PUT`  | `/profile`         | Update profile               |
| `POST` | `/signup`          | Register customer account    |
| `POST` | `/change-password` | Change password              |

### Dashboard (`/api/v1/dashboard`)

| Method | Endpoint | Description                                               |
|--------|----------|-----------------------------------------------------------|
| `GET`  | `/`      | Role-aware dashboard (CUSTOMER, AGENT, SUPERVISOR, ADMIN) |

### Admin (`/api/v1/admin`)

Model registry management, policy configuration, user management, queue memberships, audit log queries.

### Supervisor (`/api/v1/supervisor/escalations`)

Escalation listing, assignment, and resolution.

### Feedback (`/api/v1/feedback`)

Submit and query resolution feedback.

---

## Agent Runtime

The agent runtime implements a **Plan-Safety-Act-Reflect** loop using LangGraph4j:

### State Machine Nodes

| Node               | Purpose                                                                                                                                                           |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PLAN**           | Calls the planner LLM to decide the next action (category, priority, queue, next action). Validates output against JSON schema. Auto-repairs malformed responses. |
| **SAFETY**         | Evaluates the planned action against safety constraints. Can approve, block, require escalation, or require human review.                                         |
| **TOOL_EXECUTION** | Executes the decided action: auto-reply, assign queue, escalate, ask clarifying question, use knowledge article, update customer profile, etc.                    |
| **REFLECT**        | Checks termination conditions: goal reached, budget exceeded, safety blocked, or max steps. Decides whether to loop or terminate.                                 |
| **TERMINATE**      | Persists the final routing decision, completes SSE streaming, and records the agent run trace.                                                                    |

### Orchestration Modes

- **`SINGLE_AGENT`** (default): One agent handles the full Plan-Safety-Act-Reflect loop.
- **`MULTI_AGENT_HIERARCHICAL`**: A supervisor agent delegates to role-specific specialists (TIER1, TIER2, SPECIALIST).

### Supported Actions (`NextAction` enum)

`AUTO_REPLY` · `ASK_CLARIFYING` · `ASSIGN_QUEUE` · `ESCALATE` · `HUMAN_REVIEW` · `UPDATE_CUSTOMER_PROFILE` ·
`USE_KNOWLEDGE_ARTICLE` · `USE_TEMPLATE` · `AUTO_RESOLVE`

### Termination Reasons

`GOAL_REACHED` · `BUDGET_EXCEEDED` · `SAFETY_BLOCKED` · `PLAN_VALIDATION_FAILED` · `MAX_STEPS_REACHED` · `TIMEOUT`

---

## Policy Engine

The policy engine applies ordered rules to every routing decision before execution:

| Rule                              | Trigger                                        | Effect                                                                    |
|-----------------------------------|------------------------------------------------|---------------------------------------------------------------------------|
| **SecurityContentEscalationRule** | `SECURITY` category or security rationale tags | Overrides queue to `SECURITY_Q`, priority to `HIGH`, action to `ESCALATE` |
| **CriticalMinConfidenceRule**     | `CRITICAL` priority + confidence < 0.85        | Overrides to `HUMAN_REVIEW`                                               |
| **AutoRouteThresholdRule**        | Confidence < 0.70                              | Overrides to `HUMAN_REVIEW`                                               |

Policy thresholds are configurable via the `policy_config` table and admin API.

---

## Resilience

The system is designed for graceful degradation when the LLM is unavailable or unreliable:

- **Circuit Breaker** (`llmCircuit`): Opens after 50% failure rate over 10 calls. Waits 30s before half-open. Prevents
  cascading failures.
- **Retry** (`llmRetry`): Up to 3 attempts with exponential backoff (1s, 2s, 4s).
- **Time Limiter**: 600s timeout for LLM inference.
- **Fallback**: When the circuit is open or all retries exhausted, returns a safe fallback routing decision (routes to
  `GENERAL_Q` with `HUMAN_REVIEW`).
- **Auto-Repair**: When the LLM returns malformed JSON, the system attempts schema-guided repair before falling back.

---

## Frontend

The React SPA is built with Vite and served from `/app/` by Spring Boot.

### Development

```bash
cd frontend
npm install
npm run dev          # Dev server with HMR
npm run build        # Production build → src/main/resources/static/app/
npm run lint         # ESLint
npm run typecheck    # TypeScript strict check
```

### Key Libraries

- **Radix UI** — accessible component primitives (dialog, dropdown, select, tooltip, avatar)
- **TanStack Query** — server state management with caching
- **React Hook Form + Zod** — form handling with schema validation
- **Axios** — HTTP client with interceptors
- **Lucide React** — icon system
- **Sonner** — toast notifications

### Pages

- Dashboard (role-aware) · Ticket list · Ticket detail (with SSE routing/draft streaming)
- New ticket · Queue view · Review queue
- Escalations · Admin (models, policies, users) · Audit log
- Login · Signup · Account settings

---

## Default Users

All passwords: `AIClub123`

| Username         | Role       | Description                                |
|------------------|------------|--------------------------------------------|
| `admin`          | ADMIN      | System administration, model/policy config |
| `supervisor1`    | SUPERVISOR | Escalation handling, routing review        |
| `agent_billing`  | AGENT      | Billing queue agent                        |
| `agent_tech`     | AGENT      | Technical support agent                    |
| `agent_security` | AGENT      | Security queue agent                       |
| `agent_general`  | AGENT      | General support agent                      |

Customers can self-register via the signup page.

---

## Documentation

Comprehensive technical documentation is available in [`docs/agentic-system/`](docs/agentic-system/):

| Document                                                                    | Topic                                                             |
|-----------------------------------------------------------------------------|-------------------------------------------------------------------|
| [01 - Overview](docs/agentic-system/01-overview.md)                         | Executive summary, what makes it "agentic", tech stack comparison |
| [02 - Architecture](docs/agentic-system/02-architecture.md)                 | C4 diagrams, component design, ADRs                               |
| [03 - Routing Flow](docs/agentic-system/03-routing-flow.md)                 | End-to-end sequence diagrams                                      |
| [04 - State Machine](docs/agentic-system/04-state-machine.md)               | LangGraph4j graph structure and state transitions                 |
| [05 - Multi-Agent](docs/agentic-system/05-multi-agent.md)                   | Supervisor-specialist delegation                                  |
| [06 - Policy Engine](docs/agentic-system/06-policy-engine.md)               | Rule structure, built-in rules, extensibility                     |
| [07 - Resilience](docs/agentic-system/07-resilience.md)                     | Circuit breaker, retry, fallback patterns                         |
| [08 - Observability](docs/agentic-system/08-observability.md)               | Run/step tracing, SSE streaming, structured logging               |
| [09 - Configuration](docs/agentic-system/09-configuration.md)               | Full configuration reference                                      |
| [10 - Troubleshooting](docs/agentic-system/10-troubleshooting.md)           | Common issues, error codes, debugging                             |
| [11 - Performance Tuning](docs/agentic-system/11-performance-tuning.md)     | LLM, database, caching optimization                               |
| [12 - Production Checklist](docs/agentic-system/12-production-checklist.md) | Pre-launch, monitoring, alerting setup                            |

---

## Useful Commands

```bash
# Database
docker compose up -d              # Start PostgreSQL + pgvector
docker compose down               # Stop database

# Ollama
ollama serve                      # Start LLM server
ollama list                       # List pulled models

# Backend
./mvnw clean compile              # Compile
./mvnw spring-boot:run            # Run application
./mvnw clean package              # Build JAR
./mvnw test                       # Run tests

# Frontend
cd frontend && npm run dev        # Dev server with HMR
cd frontend && npm run build      # Production build
```
