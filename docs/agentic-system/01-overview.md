# 01 - Overview

> Executive summary and introduction to the Agentic Ticket Router system.

---

## Table of Contents

1. [What Is This System?](#1-what-is-this-system)
2. [What Makes It "Agentic"?](#2-what-makes-it-agentic)
3. [Technology Stack](#3-technology-stack)
4. [Key Capabilities](#4-key-capabilities)
5. [System Context](#5-system-context)
6. [High-Level Architecture](#6-high-level-architecture)
7. [Teaching Checklist](#7-teaching-checklist)

---

## 1. What Is This System?

The **Agentic Ticket Router** is an AI-powered support ticket routing system that goes beyond simple LLM-based
classification. It uses a multi-step state machine with:

- **Autonomous reasoning** - The system can take multiple actions without human intervention
- **Multi-agent delegation** - A supervisor agent delegates to specialists
- **Policy-based guardrails** - Safety rules prevent dangerous or low-confidence decisions
- **Full observability** - Every decision is traced and explainable
- **Graceful degradation** - Falls back to human review when uncertain

### Business Value

| Capability            | Business Impact                                    |
|-----------------------|----------------------------------------------------|
| Auto-routing          | Reduces manual triage by routing to correct queues |
| Auto-reply            | Resolves simple tickets without agent involvement  |
| Priority detection    | Ensures critical issues get attention fast         |
| Security escalation   | Automatically flags and escalates security issues  |
| Human review triggers | Prevents AI mistakes from reaching customers       |

---

## 2. What Makes It "Agentic"?

### Traditional LLM Application

```
┌─────────────────────────────────────────────────────────┐
│                    TRADITIONAL APPROACH                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   User Input ──→ LLM ──→ Response                       │
│                                                         │
│   Problems:                                             │
│   • Single shot - no iteration or refinement            │
│   • No memory between steps                             │
│   • No validation of output                             │
│   • No guardrails on decisions                          │
│   • Cannot take real actions                            │
│   • Fails silently                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Agentic System

```
┌─────────────────────────────────────────────────────────┐
│                     AGENTIC APPROACH                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌────────┐  │
│   │  PLAN   │──→│ SAFETY  │──→│  TOOL   │──→│REFLECT │  │
│   └─────────┘   └─────────┘   │  EXEC   │   └────────┘  │
│       ↑                       └─────────┘       ↑       │
│       │                                         │       │
│       └─────────────────────────────────────────┘       │
│                   (loop if needed)                      │
│                                                         │
│   Benefits:                                             │
│   • Multi-step reasoning with state                     │
│   • Output validation + auto-repair                     │
│   • Policy gates prevent bad decisions                  │
│   • Executes real actions on the system                 │
│   • Full traceability and explainability                │
│   • Graceful fallback to human review                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Comparison Matrix

| Feature           | Traditional LLM | This System                        |
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

## 3. Technology Stack

### Layer Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   React/TS      │  │   Thymeleaf     │  │   REST API   │ │
│  │   Frontend      │  │   Templates     │  │   Endpoints  │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Spring Boot   │  │   Spring MVC    │  │ Spring       │ │
│  │   Framework     │  │   Controllers   │  │ Security RBAC│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       AGENTIC LAYER                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  LangGraph4j    │  │  Agent Runtime  │  │ Policy Engine│ │
│  │  State Machine  │  │  Orchestrator   │  │  Guardrails  │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  Tool Executor  │  │  Multi-Agent    │  │ Safety Eval  │ │
│  │  Actions        │  │  Delegation     │  │  Engine      │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         AI/ML LAYER                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Spring AI     │  │   Ollama/       │  │   pgvector   │ │
│  │   Integration   │  │   OpenAI        │  │   RAG Store  │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │   Prompt        │  │   Schema        │                   │
│  │   Templates     │  │   Validator     │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  PostgreSQL     │  │  Resilience4j   │  │ Spring Events│ │
│  │  + JSONB        │  │  Circuit Breaker│  │   Async      │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │  SSE Engine     │  │  HikariCP       │                   │
│  │  Real-time      │  │  Connection Pool│                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### Component Details

| Layer              | Component     | Purpose                               |
|--------------------|---------------|---------------------------------------|
| **AI/ML**          | Spring AI     | Unified interface for LLM providers   |
| **AI/ML**          | Ollama        | Local LLM inference (development)     |
| **AI/ML**          | OpenAI        | Cloud LLM inference (production)      |
| **AI/ML**          | pgvector      | Vector similarity search for RAG      |
| **Agentic**        | LangGraph4j   | State machine orchestration           |
| **Agentic**        | Policy Engine | Rule-based guardrails                 |
| **Infrastructure** | Resilience4j  | Circuit breaker, retry, rate limiting |
| **Infrastructure** | Spring Events | Async event-driven processing         |
| **Infrastructure** | SSE           | Real-time progress to frontend        |

---

## 4. Key Capabilities

### 4.1 Autonomous Ticket Routing

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   Ticket Created                                           │
│        │                                                   │
│        ▼                                                   │
│   ┌─────────┐     ┌──────────┐     ┌──────────────┐        │
│   │ Classify│ ──→ │ Priority │ ──→ │   Assign     │        │
│   │ Category│     │ Detection│     │   to Queue   │        │
│   └─────────┘     └──────────┘     └──────────────┘        │
│                                            │               │
│                                            ▼               │
│                                    ┌──────────────┐        │
│                                    │  BILLING_Q   │        │
│                                    │  TECH_Q      │        │
│                                    │  SECURITY_Q  │        │
│                                    │  etc.        │        │
│                                    └──────────────┘        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 4.2 Automatic Reply Generation

When confidence is high and the issue is simple:

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   Customer: "How do I reset my password?"                  │
│                                                            │
│   System:                                                  │
│   ┌─────────────────────────────────────────────────────┐  │
│   │ 1. Search knowledge base for "password reset"       │  │
│   │ 2. Find matching article                            │  │
│   │ 3. Generate personalized response                   │  │
│   │ 4. Draft reply for agent review OR auto-send        │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                            │
│   Response: "Hi [Customer], to reset your password..."     │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 4.3 Clarifying Questions

When ticket context is ambiguous:

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   Customer: "It's not working"                             │
│                                                            │
│   System detects:                                          │
│   • Low confidence (0.35)                                  │
│   • Missing context (what's not working?)                  │
│   • Budget remaining for questions (3 max)                 │
│                                                            │
│   Auto-response:                                           │
│   "I'd be happy to help! Could you tell me:                │
│    1. What specific feature isn't working?                 │
│    2. Are you seeing any error messages?                   │
│    3. When did this start happening?"                      │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 4.4 Security Escalation

Automatic detection and escalation:

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   Customer: "I think someone accessed my account"          │
│                                                            │
│   System detects:                                          │
│   • Category: SECURITY                                     │
│   • OR rationale tags: ["account_access", "unauthorized"]  │
│                                                            │
│   Automatic actions:                                       │
│   ┌─────────────────────────────────────────────────────┐  │
│   │ 1. Override queue → SECURITY_Q                      │  │
│   │ 2. Override priority → HIGH                         │  │
│   │ 3. Override action → ESCALATE                       │  │
│   │ 4. Notify on-call security team                     │  │
│   │ 5. Add internal note with details                   │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 5. System Context

### C4 Context Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│     ┌───────────────┐                                                   │
│     │   Customer    │                                                   │
│     │               │                                                   │
│     │  Submits      │                                                   │
│     │  tickets,     │                                                   │
│     │  receives     │                                                   │
│     │  responses    │                                                   │
│     └───────┬───────┘                                                   │
│             │                                                           │
│             │                                                           │
│             ▼                                                           │
│     ┌───────────────────────────────────────────────────────────┐       │
│     │                                                           │       │
│     │              AGENTIC TICKET ROUTER                        │       │
│     │                                                           │       │
│     │  • Routes tickets to appropriate queues                   │       │
│     │  • Generates draft replies                                │       │
│     │  • Detects and escalates security issues                  │       │
│     │  • Asks clarifying questions                              │       │
│     │  • Provides decision traceability                         │       │
│     │                                                           │       │
│     └───────────┬───────────────────────────────┬───────────────┘       │
│                 │                               │                       │
│                 │                               │                       │
│                 ▼                               ▼                       │
│     ┌───────────────────┐           ┌───────────────────┐               │
│     │   Support Agent   │           │   LLM Provider    │               │
│     │                   │           │                   │               │
│     │  Reviews AI       │           │  Ollama (local)   │               │
│     │  decisions,       │           │  or               │               │
│     │  handles human    │           │  OpenAI (cloud)   │               │
│     │  review queue     │           │                   │               │
│     └───────────────────┘           └───────────────────┘               │
│                                                                         │
│     ┌───────────────────┐           ┌───────────────────┐               │
│     │   Supervisor      │           │   Admin           │               │
│     │                   │           │                   │               │
│     │  Handles          │           │  Configures       │               │
│     │  escalations,     │           │  policies,        │               │
│     │  reviews routing  │           │  models,          │               │
│     │  decisions        │           │  monitors system  │               │
│     └───────────────────┘           └───────────────────┘               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. High-Level Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AGENTIC TICKET ROUTER                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      ENTRY POINTS                               │   │
│   │  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐    │   │
│   │  │ REST API      │  │ MVC           │  │ Event Listener    │    │   │
│   │  │ /api/v1/*     │  │ /app/*        │  │ TicketCreatedEvent│    │   │
│   │  └───────────────┘  └───────────────┘  └───────────────────┘    │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      ROUTING LAYER                              │   │
│   │  ┌───────────────┐  ┌───────────────┐  ┌───────────────────┐    │   │
│   │  │ Router        │  │ Routing       │  │ Strategy          │    │   │
│   │  │ Orchestrator  │  │ Coordinator   │  │ Selection         │    │   │
│   │  │ @Async        │  │               │  │                   │    │   │
│   │  └───────────────┘  └───────────────┘  └───────────────────┘    │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                          ┌─────────┴─────────┐                          │
│                          ▼                   ▼                          │
│   ┌────────────────────────────┐  ┌────────────────────────────┐        │
│   │   AGENT RUNTIME STRATEGY   │  │   CLASSIC ROUTING STRATEGY │        │
│   │                            │  │                            │        │
│   │   LangGraph4j State Machine│  │   Single LLM Call          │        │
│   │   Multi-step reasoning     │  │   Policy Engine            │        │
│   │   Multi-agent delegation   │  │   Action Execution         │        │
│   │                            │  │                            │        │
│   └────────────────────────────┘  └────────────────────────────┘        │
│                          │                                              │
│                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      SHARED SERVICES                            │   │
│   │                                                                 │   │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │   │
│   │  │ Policy      │ │ Tool        │ │ Context     │ │ Vector    │  │   │
│   │  │ Engine      │ │ Executor    │ │ Enrichment  │ │ Store     │  │   │
│   │  │             │ │             │ │ (RAG)       │ │ (pgvector)│  │   │
│   │  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │   │
│   │                                                                 │   │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │   │
│   │  │ LLM Client  │ │ Trace       │ │ SSE Engine  │ │ Fallback  │  │   │
│   │  │ (Spring AI) │ │ Service     │ │             │ │ Service   │  │   │
│   │  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │   │
│   │                                                                 │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      DATA LAYER                                 │   │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐  │   │
│   │  │ PostgreSQL  │ │ Agent       │ │ Ticket      │ │ LLM       │  │   │
│   │  │ + JSONB     │ │ Runtime     │ │ Routing     │ │ Output    │  │   │
│   │  │ + pgvector  │ │ Run/Step    │ │ History     │ │ Logs      │  │   │
│   │  └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘  │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Teaching Checklist

### Preparation

- [ ] Have a running instance with sample data
- [ ] Enable shadow mode for safe demonstrations
- [ ] Open database to show `agent_runtime_*` tables
- [ ] Open browser DevTools to show SSE events
- [ ] Prepare sample tickets of different types:
    - [ ] Simple billing question (should auto-route)
    - [ ] Ambiguous issue (should ask clarifying)
    - [ ] Security concern (should escalate)
    - [ ] High-priority issue (should require high confidence)

### Teaching Flow

#### Module 1: Introduction (15 min)

1. [ ] Explain what "agentic" means vs traditional LLM
2. [ ] Show the comparison matrix
3. [ ] Walk through the C4 context diagram
4. [ ] Demonstrate a simple ticket routing

#### Module 2: Architecture (20 min)

1. [ ] Draw the component overview on whiteboard
2. [ ] Explain the entry points (REST, MVC, Events)
3. [ ] Show the two strategies (Agent Runtime vs Classic)
4. [ ] Explain shared services and their roles

#### Module 3: Live Demo (30 min)

1. [ ] Create a simple ticket, trace the flow
2. [ ] Show database records created:
    - `support_ticket` (status = TRIAGING → ASSIGNED)
    - `ticket_routing` (routing decision)
    - `agent_runtime_run` (execution trace)
    - `agent_runtime_step` (each node)
3. [ ] Show SSE events in DevTools
4. [ ] Create a security ticket, show forced escalation

#### Module 4: Q&A (15 min)

Prepare answers for:

- [ ] Why `AFTER_COMMIT` for the event listener?
- [ ] What happens if LLM returns invalid JSON?
- [ ] When would the state machine loop?
- [ ] How do we prevent runaway autonomous actions?
- [ ] What's the safe default when everything fails?
- [ ] How does multi-agent delegation work?
- [ ] What triggers human review?

### Key Takeaways

After this session, learners should understand:

1. **Agentic vs Traditional**: Multi-step reasoning with state
2. **Safety First**: Multiple guardrails before any action
3. **Observability**: Every decision is traceable
4. **Graceful Degradation**: Always falls back to human review
5. **Separation of Concerns**: Strategy pattern allows A/B testing

---

## Navigation

**Previous:** [Index](./README.md)  
**Next:** [02 - Architecture](./02-architecture.md)
