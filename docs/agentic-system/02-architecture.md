# 02 - Architecture

> Detailed system architecture, design decisions, and component interactions.

---

## Table of Contents

1. [Architecture Style](#1-architecture-style)
2. [Component Diagram](#2-component-diagram)
3. [Key Packages](#3-key-packages)
4. [Data Flow](#4-data-flow)
5. [Event-Driven Design](#5-event-driven-design)
6. [Transaction Boundaries](#6-transaction-boundaries)
7. [Concurrency Model](#7-concurrency-model)
8. [Architecture Decision Records](#8-architecture-decision-records)
9. [Database Schema](#9-database-schema)

---

## 1. Architecture Style

This system uses a **Layered Architecture** with **Event-Driven** extensions:

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│    ┌───────────────────────────────────────────────────────┐    │
│    │                 PRESENTATION LAYER                    │    │
│    │        (Controllers, Templates, API Endpoints)        │    │
│    └───────────────────────────────────────────────────────┘    │
│                              │                                  │
│                              ▼                                  │
│    ┌───────────────────────────────────────────────────────┐    │
│    │                   SERVICE LAYER                       │    │
│    │   (Business Logic, Routing, Agent Runtime, Policies)  │    │
│    └───────────────────────────────────────────────────────┘    │ 
│                              │                                  │
│                              ▼                                  │
│    ┌───────────────────────────────────────────────────────┐    │
│    │                 REPOSITORY LAYER                      │    │
│    │           (JPA Repositories, Data Access)             │    │
│    └───────────────────────────────────────────────────────┘    │ 
│                              │                                  │
│                              ▼                                  │
│    ┌───────────────────────────────────────────────────────┐    │
│    │                   ENTITY LAYER                        │    │
│    │          (Domain Objects, JPA Entities)               │    │
│    └───────────────────────────────────────────────────────┘    │
│                                                                 │
│    ══════════════════════════════════════════════════════       │
│    CROSS-CUTTING: Events, Security, Logging, Caching            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why This Style?

| Concern                | Solution                         |
|------------------------|----------------------------------|
| Separation of concerns | Clear layer boundaries           |
| Testability            | Services can be mocked           |
| Flexibility            | Strategy pattern for routing     |
| Scalability            | Async event processing           |
| Maintainability        | Package by feature within layers |

---

## 2. Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AGENTIC TICKET ROUTER                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────── ENTRY ────────────────────────────────┐    │
│  │                                                                     │    │
│  │   ┌────────────────┐                      ┌────────────────────┐    │    │
│  │   │ TicketApi      │                      │ TicketRouting      │    │    │
│  │   │ Controller     │                      │ Listener           │    │    │
│  │   │ (REST)         │                      │ (Event Handler)    │    │    │
│  │   └───────┬────────┘                      └─────────┬──────────┘    │    │
│  │           │                                         │               │    │
│  └───────────┼─────────────────────────────────────────┼───────────────┘    │
│              │                                         │                    │
│              └─────────────────────────────────────────┘                    │
│                                  │                                          │
│                                  ▼                                          │
│  ┌────────────────────────── ORCHESTRATION ────────────────────────────┐    │
│  │                                                                     │    │
│  │   ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐    │    │
│  │   │ Router         │  │ Routing        │  │ RoutingRequest     │    │    │
│  │   │ Orchestrator   │──│ Execution      │──│ Factory            │    │    │
│  │   │                │  │ Coordinator    │  │ (Context Builder)  │    │    │
│  │   └────────────────┘  └───────┬────────┘  └────────────────────┘    │    │
│  │                               │                                     │    │
│  └───────────────────────────────┼─────────────────────────────────────┘    │
│                                  │                                          │
│              ┌───────────────────┼───────────────────┐                      │
│              │                   │                   │                      │
│              ▼                   ▼                   ▼                      │
│  ┌────────────────────────── STRATEGIES ───────────────────────────────┐    │
│  │                                                                     │    │
│  │   ┌────────────────────────┐   ┌────────────────────────────────┐   │    │
│  │   │ AgentRuntimeRouting    │   │ ClassicRoutingStrategy         │   │    │
│  │   │ Strategy               │   │                                │   │    │
│  │   │                        │   │  ┌────────────────────────┐    │   │    │
│  │   │  Uses LangGraph4j      │   │  │ TicketRouterService    │    │   │    │
│  │   │  State Machine         │   │  │ (Single LLM Call)      │    │   │    │
│  │   │                        │   │  └────────────────────────┘    │   │    │
│  │   └────────────┬───────────┘   └────────────────────────────────┘   │    │
│  │                │                                                    │    │
│  └────────────────┼────────────────────────────────────────────────────┘    │
│                   │                                                         │
│                   ▼                                                         │
│  ┌────────────────────────── AGENT RUNTIME ─────────────────────────────┐   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────────────┐    │   │
│  │   │                 AgentRuntimeOrchestrator                    │    │   │
│  │   │                                                             │    │   │
│  │   │   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │    │   │
│  │   │   │  PLAN   │──▶ SAFETY  │──▶TOOL_EXEC│──▶ REFLECT │        │    │   │
│  │   │   └─────────┘  └─────────┘  └─────────┘  └────┬────┘        │    │   │
│  │   │        ▲                                      │             │    │   │
│  │   │        └──────────────────────────────────────┘             │    │   │
│  │   │                     State Machine                           │    │   │
│  │   └─────────────────────────────────────────────────────────────┘    │   │
│  │                                                                      │   │
│  │   ┌────────────────┐ ┌────────────────┐ ┌────────────────┐           │   │
│  │   │ AgentPlanner   │ │ AgentSafety    │ │ AgentTool      │           │   │
│  │   │ Client         │ │ Evaluator      │ │ Executor       │           │   │
│  │   └────────────────┘ └────────────────┘ └────────────────┘           │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌──────────────────────────── SERVICES ────────────────────────────────┐   │
│  │                                                                      │   │
│  │   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │   │
│  │   │ Policy      │ │ Knowledge   │ │ Customer    │ │ Routing     │    │   │
│  │   │ Engine      │ │ Base Vector │ │ Context     │ │ Pattern     │    │   │
│  │   │             │ │ Store       │ │ Enrichment  │ │ Matcher     │    │   │
│  │   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │   │
│  │                                                                      │   │
│  │   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │   │
│  │   │ LLM Output  │ │ Agent       │ │ SSE         │ │ Model       │    │   │
│  │   │ Service     │ │ Trace       │ │ Engine      │ │ Service     │    │   │
│  │   │             │ │ Service     │ │             │ │             │    │   │
│  │   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌────────────────────────── ACTIONS ───────────────────────────────────┐   │
│  │                                                                      │   │
│  │   ┌─────────────────────────────────────────────────────────────┐    │   │
│  │   │                      ActionRegistry                         │    │   │
│  │   │                                                             │    │   │
│  │   │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐│    │   │
│  │   │  │ AutoReply  │ │ AssignQueue│ │ Escalate   │ │ HumanReview││    │   │
│  │   │  │ Action     │ │ Action     │ │ Action     │ │ Action     ││    │   │
│  │   │  └────────────┘ └────────────┘ └────────────┘ └────────────┘│    │   │
│  │   │                                                             │    │   │
│  │   │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐│    │   │
│  │   │  │ Ask        │ │ Change     │ │ Update     │ │ Add        ││    │   │
│  │   │  │ Clarifying │ │ Priority   │ │ Profile    │ │ Note       ││    │   │
│  │   │  │ Action     │ │ Action     │ │ Action     │ │ Action     ││    │   │
│  │   │  └────────────┘ └────────────┘ └────────────┘ └────────────┘│    │   │
│  │   └─────────────────────────────────────────────────────────────┘    │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Key Packages

### Package Structure

```
com.dsi.support.agenticrouter
│
├── configuration/           # Spring configuration classes
│   ├── AgentRuntimeConfiguration.java
│   ├── SecurityConfiguration.java
│   ├── AsyncConfiguration.java
│   └── ...
│
├── controller/              # Entry points
│   ├── api/                 # REST controllers
│   │   ├── TicketApiController.java
│   │   ├── FeedbackApiController.java
│   │   └── ...
│   ├── mvc/                 # Web MVC controllers
│   │   ├── TicketMvcController.java
│   │   └── ...
│   └── ...
│
├── dto/                     # Data Transfer Objects
│   ├── RouterRequest.java
│   ├── RouterResponse.java
│   └── ...
│
├── entity/                  # JPA entities
│   ├── SupportTicket.java
│   ├── AgentRuntimeRun.java
│   ├── AgentRuntimeStep.java
│   └── ...
│
├── enums/                   # Enumerations with behavior
│   ├── NextAction.java
│   ├── AgentRole.java
│   ├── TicketPriority.java
│   └── ...
│
├── event/                   # Domain events
│   ├── TicketCreatedEvent.java
│   ├── FeedbackCapturedEvent.java
│   └── ...
│
├── listener/                # Event listeners
│   ├── TicketRoutingListener.java
│   └── ...
│
├── repository/              # JPA repositories
│   ├── SupportTicketRepository.java
│   ├── AgentRuntimeRunRepository.java
│   └── ...
│
├── service/                 # Business logic
│   ├── routing/             # Routing orchestration
│   │   ├── RouterOrchestrator.java
│   │   ├── RoutingExecutionCoordinator.java
│   │   ├── RoutingExecutionStrategy.java
│   │   ├── TicketRouterService.java
│   │   └── policy/          # Policy rules
│   │       ├── PolicyEngine.java
│   │       ├── AutoRouteThresholdRule.java
│   │       └── ...
│   │
│   ├── agentruntime/        # Agent runtime components
│   │   ├── orchestration/   # State machine
│   │   │   ├── AgentRuntimeOrchestrator.java
│   │   │   ├── AgentGraphState.java
│   │   │   └── ...
│   │   ├── planner/         # LLM planning
│   │   │   ├── AgentPlannerClient.java
│   │   │   └── ...
│   │   ├── safety/          # Safety evaluation
│   │   │   ├── AgentSafetyEvaluator.java
│   │   │   └── ...
│   │   ├── tooling/         # Tool execution
│   │   │   ├── AgentToolExecutor.java
│   │   │   └── ...
│   │   ├── trace/           # Run tracing
│   │   └── rollout/         # Canary deployment
│   │
│   ├── action/              # Action handlers
│   │   ├── ActionRegistry.java
│   │   ├── TicketAction.java
│   │   └── handlers/
│   │       ├── AutoReplyAction.java
│   │       └── ...
│   │
│   ├── ai/                  # AI/ML services
│   │   ├── ModelService.java
│   │   ├── LlmOutputService.java
│   │   └── ...
│   │
│   └── ...
│
├── exception/               # Custom exceptions
│   ├── DataNotFoundException.java
│   ├── RoutingExecutionException.java
│   └── ...
│
└── util/                    # Utilities
    ├── OperationalLogContext.java
    └── ...
```

### Design Principles Applied

| Principle                 | Application                        |
|---------------------------|------------------------------------|
| **Single Responsibility** | Each service has one job           |
| **Open/Closed**           | Add rules without modifying engine |
| **Dependency Inversion**  | Depend on interfaces (Strategy)    |
| **Interface Segregation** | Small, focused interfaces          |
| **Package by Feature**    | Grouped by domain concept          |

---

## 4. Data Flow

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            TICKET CREATION FLOW                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Customer        TicketApi        TicketCreation      SupportTicket         │
│     │               │                  │                   │                │
│     │──Submit──────▶│                  │                   │                │
│     │               │──createTicket───▶│                   │                │
│     │               │                  │──save────────────▶│                │
│     │               │                  │                   │                │
│     │               │                  │◀─────saved────────│                │
│     │               │                  │                   │                │
│     │               │                  │──publish──────────┐                │
│     │               │                  │ TicketCreatedEvent│                │
│     │               │                  │                   │                │
│     │               │◀───success───────│                   │                │
│     │◀─────show─────│                  │                   │                │
│     │    success    │                  │                   │                │
│     │               │                  │                   │                │
└─────┴───────────────┴──────────────────┴───────────────────┴────────────────┘
                                   │ (After Commit)
                                   ▼

┌─────────────────────────────────────────────────────────────────────────────┐
│                              ASYNC ROUTING FLOW                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TicketCreated    TicketRouting     Router         RoutingRequest           │
│  Event               Listener       Orchestrator      Factory               │
│     │                   │               │                │                  │
│     │──handle──────────▶│               │                │                  │
│     │                   │──routeTicket─▶│                │                  │
│     │                   │  @Async       │                │                  │
│     │                   │               │──buildRequest─▶│                  │
│     │                   │               │                │──load messages   │
│     │                   │               │                │──search articles │
│     │                   │               │                │──find patterns   │
│     │                   │               │                │                  │
│     │                   │               │◀──RouterRequest│                  │
│     │                   │               │                │                  │
│     │                   │               │──execute───────┐                  │
│     │                   │               │                │                  │
└─────┴───────────────────┴───────────────┴────────────────┴──────────────────┘
                                   │
                                   ▼

┌─────────────────────────────────────────────────────────────────────────────┐
│                           STRATEGY EXECUTION FLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  RoutingExec     AgentRuntime     StateMachine    LLM        PolicyEngine   │
│  Coordinator      Strategy         Nodes                                    │
│     │                 │               │             │                │      │
│     │──execute───────▶│               │             │                │      │
│     │                 │               │             │                │      │
│     │                 │──PLAN────────▶│──decide────▶│                │      │
│     │                 │               │◀──response─ │                │      │
│     │                 │               │             │                │      │
│     │                 │               │──SAFETY─────────────────────▶│      │
│     │                 │               │◀─────safe response───────    │      │
│     │                 │               │           │                  │      │
│     │                 │               │──TOOL_EXEC (execute action)  │      │
│     │                 │               │           │                  │      │
│     │                 │               │──REFLECT (continue?)         │      │
│     │                 │               │           │                  │      │
│     │                 │               │──TERMINATE                   │      │
│     │                 │               │           │                  │      │
│     │◀────result──────│               │           │                  │      │
│     │                 │               │           │                  │      │
└─────┴─────────────────┴───────────────┴───────────┴──────────────────┴──────┘
```

---

## 5. Event-Driven Design

### Why Events?

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   WITHOUT EVENTS (Synchronous)                                  │
│   ─────────────────────────────                                 │
│                                                                 │
│   API Call ──→ Create Ticket ──→ Route Ticket ──→ Return        │
│                                   │                             │
│                                   └── blocks response           │
│                                                                 │
│   Problem: Routing takes 5-15 seconds                           │
│   Customer waits for response...                                │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   WITH EVENTS (Asynchronous)                                    │
│   ──────────────────────────                                    │
│                                                                 │
│   API Call ──→ Create Ticket ──→ Return ──→ (fast)              │
│                      │                                          │
│                      └──→ Event Published                       │
│                               │                                 │
│                               └──→ Listener routes async        │
│                                                                 │
│   Benefit: Customer gets instant feedback                       │
│   Routing happens in background                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│   ┌─────────────────┐                                                   │
│   │ Ticket Creation │                                                   │
│   │ Service         │                                                   │
│   └────────┬────────┘                                                   │
│            │                                                            │
│            │ publish                                                    │
│            ▼                                                            │
│   ┌─────────────────┐     ┌─────────────────┐                           │
│   │ Spring          │────>│ Event Bus       │                           │
│   │ ApplicationEvent│     │ (In-Memory)     │                           │
│   │ Publisher       │     └────────┬────────┘                           │
│   └─────────────────┘              │                                    │
│                                    │                                    │
│           ┌────────────────────────┼────────────────────────┐           │
│           │                        │                        │           │
│           ▼                        ▼                        ▼           │
│   ┌───────────────┐      ┌───────────────┐      ┌───────────────┐       │
│   │ TicketRouting │      │ AuditLog      │      │ Notification  │       │
│   │ Listener      │      │ Listener      │      │ Listener      │       │
│   │ @Async        │      │ @Async        │      │ @Async        │       │
│   │ AFTER_COMMIT  │      │ AFTER_COMMIT  │      │ AFTER_COMMIT  │       │
│   └───────────────┐      └───────────────┐      └───────────────┐       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Event Types

| Event                   | Publisher                     | Listener              | Purpose                   |
|-------------------------|-------------------------------|-----------------------|---------------------------|
| `TicketCreatedEvent`    | TicketCreationWorkflowService | TicketRoutingListener | Trigger async routing     |
| `FeedbackCapturedEvent` | FeedbackService               | FeedbackLogListener   | Log feedback for learning |
| `TicketEscalatedEvent`  | EscalationService             | NotificationListener  | Alert supervisors         |

### Why `@TransactionalEventListener(phase = AFTER_COMMIT)`?

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   Scenario: Event published BEFORE transaction commits          │
│   ────────────────────────────────────────────────────          │
│                                                                 │
│   1. Begin transaction                                          │
│   2. Save ticket (not yet committed)                            │
│   3. Publish TicketCreatedEvent                                 │
│   4. Async listener starts in new thread                        │
│   5. Listener tries to read ticket...                           │
│   6. ┌─────────────────────────────────────────┐                │
│      │ ERROR: Ticket not visible in DB         │                │
│      │ (Transaction not committed yet)         │                │
│      └─────────────────────────────────────────┘                │
│   7. Rollback transaction (due to other error)                  │
│   8. Event was processed for non-existent ticket                │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Solution: AFTER_COMMIT                                        │
│   ─────────────────────────                                     │
│                                                                 │
│   1. Begin transaction                                          │
│   2. Save ticket                                                │
│   3. Publish TicketCreatedEvent                                 │
│   4. Commit transaction                                         │
│   5. Spring fires event (only after successful commit)          │
│   6. Async listener starts                                      │
│   7. Listener reads ticket successfully                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Transaction Boundaries

### Transaction Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│   REQUEST TRANSACTION (Read-Write)                                      │
│   ────────────────────────────────                                      │
│                                                                         │
│   ┌─────────────────┐                                                   │
│   │ Controller      │                                                   │
│   └────────┬────────┘                                                   │
│            │ @Transactional                                             │
│            ▼                                                            │
│   ┌─────────────────┐                                                   │
│   │ Service Method  │  ← Creates/updates ticket                         │
│   └────────┬────────┘                                                   │
│            │                                                            │
│            │ Commit after return                                        │
│            ▼                                                            │
│   ┌─────────────────┐                                                   │
│   │ Event Published │  ← AFTER_COMMIT listener fires                    │
│   └─────────────────┘                                                   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ROUTING TRANSACTION (Read-Write, Isolated)                            │
│   ────────────────────────────────────────────                          │
│                                                                         │
│   ┌─────────────────┐                                                   │
│   │ Event Listener  │                                                   │
│   └────────┬────────┘                                                   │
│            │ (No transaction - just dispatch)                           │
│            ▼                                                            │
│   ┌─────────────────┐                                                   │
│   │ Router          │  @Async + @Transactional(REQUIRES_NEW)            │
│   │ Orchestrator    │                                                   │
│   └────────┬────────┘                                                   │
│            │                                                            │
│            │ New transaction starts here                                │
│            │ Prevents lock contention with request transaction          │
│            ▼                                                            │
│   ┌─────────────────┐                                                   │
│   │ Update ticket   │  ← Status = TRIAGING                              │
│   │ Execute routing │  ← Full state machine                             │
│   │ Apply decision  │  ← Update ticket with results                     │
│   └────────┬────────┘                                                   │
│            │                                                            │
│            │ Commit                                                     │
│            ▼                                                            │
│   ┌─────────────────┐                                                   │
│   │ Transaction End │                                                   │
│   └─────────────────┘                                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Why `REQUIRES_NEW`?

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   Without REQUIRES_NEW (Joins existing transaction):            │
│   ──────────────────────────────────────────────────            │
│                                                                 │
│   [Request Thread]                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Transaction A (Request)                                 │   │
│   │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │   │
│   │ │ Create      │ │ Publish     │ │ Wait for    │         │   │
│   │ │ Ticket      │ │ Event       │ │ Listener    │         │   │
│   │ └─────────────┘ └─────────────┘ └─────────────┘         │   │
│   │ (Transaction still open - holding locks)                │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   [Async Thread]                                                │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Joins Transaction A...                                  │   │
│   │ Problem: Locks held by Request thread                   │   │
│   │ Problem: Long transaction (5-15 seconds)                │   │
│   │ Problem: If routing fails, ticket creation rolls back   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   With REQUIRES_NEW (Independent transaction):                  │
│   ────────────────────────────────────────────                  │
│                                                                 │
│   [Request Thread]                                              │
│   ┌─────────────────────┐                                       │
│   │ Transaction A       │                                       │
│   │ ┌─────────────────┐ │                                       │
│   │ │ Create Ticket   │ │                                       │
│   │ └─────────────────┘ │                                       │
│   │ Commit              │                                       │
│   └─────────────────────┘                                       │
│                                                                 │
│   [Async Thread]                                                │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Transaction B (NEW - independent)                       │   │
│   │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │   │
│   │ │ Load Ticket │ │ Route       │ │ Update      │         │   │
│   │ │             │ │ (5-15s)     │ │ Results     │         │   │
│   │ └─────────────┘ └─────────────┘ └─────────────┘         │   │
│   │ Commit                                                  │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Benefits:                                                     │
│   • Request returns immediately                                 │
│   • No lock contention                                          │
│   • Routing failure doesn't affect ticket creation              │
│   • Can retry routing independently                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Concurrency Model

### Thread Pool Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                    TOMCAT THREAD POOL                           │   │
│   │                    (HTTP Requests)                              │   │
│   │                                                                 │   │
│   │   Thread-1  Thread-2  Thread-3  ...  Thread-N                   │   │
│   │     │          │         │              │                       │   │
│   │     │          │         │              │                       │   │
│   │     ▼          ▼         ▼              ▼                       │   │
│   │   Handle HTTP requests (sync)                                   │   │
│   │   - Create tickets                                              │   │
│   │   - Update tickets                                              │   │
│   │   - API calls                                                   │   │
│   │                                                                 │   │
│   │   Size: ~200 threads (configurable)                             │   │
│   │   Purpose: Handle incoming HTTP traffic                         │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                                  │                                      │
│                                  │ @Async                               │
│                                  ▼                                      │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                 TICKET ROUTING THREAD POOL                      │   │
│   │                 (Async Processing)                              │   │
│   │                                                                 │   │
│   │   Router-1  Router-2  Router-3  ...  Router-M                   │   │
│   │      │         │         │              │                       │   │
│   │      │         │         │              │                       │   │
│   │      ▼         ▼         ▼              ▼                       │   │
│   │   Execute routing (5-15 seconds each)                           │   │
│   │   - Context enrichment                                          │   │
│   │   - LLM calls                                                   │   │
│   │   - State machine execution                                     │   │
│   │   - Action execution                                            │   │
│   │                                                                 │   │
│   │   Size: ~10 threads (sized for LLM throughput)                  │   │
│   │   Purpose: Heavy LLM operations, don't block HTTP threads       │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Why Separate Thread Pools?

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   Problem: Using Tomcat threads for routing                     │
│   ─────────────────────────────────────────                     │
│                                                                 │
│   Tomcat Pool (200 threads)                                     │
│   ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐                  │
│   │ T1   │ │ T2   │ │ T3   │ │ T4   │ │ ...  │                  │
│   │ HTTP │ │ HTTP │ │ HTTP │ │ HTTP │ │      │                  │
│   └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──────┘                  │
│      │        │        │        │                               │
│      │     ┌──┴────────┴──┐     │                               │
│      │     │ Routing (10s)│     │                               │
│      │     └──────────────┘     │                               │
│      │                          │                               │
│      └─────> Threads blocked waiting for LLM                    │
│                                                                 │
│   Result: 20 concurrent tickets = 20 blocked threads            │
│           New HTTP requests wait in queue                       │
│           Server appears unresponsive!                          │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Solution: Dedicated routing thread pool                       │
│   ───────────────────────────────────────                       │
│                                                                 │
│   Tomcat Pool (200 threads)        Routing Pool (10 threads)    │
│   ┌──────┐ ┌──────┐ ┌──────┐       ┌──────┐ ┌──────┐ ┌──────┐   │
│   │ T1   │ │ T2   │ │ T3   │       │ R1   │ │ R2   │ │ ...  │   │
│   │ HTTP │ │ HTTP │ │ HTTP │       │ Route│ │ Route│ │      │   │
│   └──┬───┘ └──┬───┘ └──┬───┘       └──────┘ └──────┘ └──────┘   │
│      │        │        │                                        │
│      │        │        │           ┌──────────────────────┐     │
│      │        │        └──────────>│ Queue (unbounded)    │     │
│      │        │                    │  Ticket 1            │     │
│      │        │                    │  Ticket 2            │     │
│      │        │                    │  Ticket 3...         │     │
│      ▼        ▼                    └──────────────────────┘     │
│   Returns immediately                                           │
│   (ticket created, routing queued)                              │
│                                                                 │
│   Result: HTTP threads free immediately                         │
│           Routing happens in background                         │
│           Server stays responsive                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# application.properties
# Routing thread pool
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
spring.task.execution.pool.keep-alive=60s
spring.task.execution.thread-name-prefix=routing-
```

---

## 8. Architecture Decision Records

### ADR-001: Why LangGraph4j?

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADR-001: State Machine Library               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Context:                                                      │
│   Need a way to orchestrate multi-step LLM reasoning            │
│                                                                 │
│   Options Considered:                                           │
│                                                                 │
│   1. Custom State Machine                                       │
│      + Full control                                             │
│      - Lots of boilerplate                                      │
│      - No visualization/debugging tools                         │
│      - Hard to add conditional edges                            │
│                                                                 │
│   2. Spring State Machine                                       │
│      + Spring integration                                       │
│      - Heavy configuration                                      │
│      - Not designed for AI workflows                            │
│      - Complex persistence                                      │
│                                                                 │
│   3. LangGraph4j                                                │
│      + Purpose-built for AI agents                              │
│      + Simple API (nodes + edges)                               │
│      + Built-in state management                                │
│      + Conditional routing                                      │
│      + Max iteration safety                                     │
│      - Less mature than alternatives                            │
│                                                                 │
│   Decision: LangGraph4j                                         │
│                                                                 │
│   Rationale:                                                    │
│   - AI-specific patterns built-in                               │
│   - Matches LangGraph (Python) patterns                         │
│   - Simpler code than custom solution                           │
│   - Good balance of power and simplicity                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### ADR-002: Why Event-Driven Routing?

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADR-002: Async Routing Pattern               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Context:                                                      │
│   Ticket routing takes 5-15 seconds (LLM latency)               │
│   Must not block user requests                                  │
│                                                                 │
│   Options Considered:                                           │
│                                                                 │
│   1. Synchronous Routing                                        │
│      + Simpler code                                             │
│      + Immediate result                                         │
│      - Blocks request 5-15s                                     │
│      - Poor user experience                                     │
│      - Timeouts likely                                          │
│                                                                 │
│   2. Background Job (Scheduled)                                 │
│      + Non-blocking                                             │
│      - Delay (polling interval)                                 │
│      - More infrastructure                                      │
│      - No immediate feedback                                    │
│                                                                 │
│   3. Event-Driven (Spring Events)                               │
│      + Non-blocking                                             │
│      + Immediate dispatch                                       │
│      + Spring native (no extra infra)                           │
│      + Decoupled from creation                                  │
│      - Requires async handling                                  │
│                                                                 │
│   Decision: Event-Driven with @Async                            │
│                                                                 │
│   Rationale:                                                    │
│   - Best user experience (instant feedback)                     │
│   - Simple implementation (Spring native)                       │
│   - Scales well (thread pool)                                   │
│   - Easy to add more listeners                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### ADR-003: Why Strategy Pattern for Routing?

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADR-003: Routing Strategy Pattern            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Context:                                                      │
│   Want to test new agent runtime alongside classic routing      │
│   Need canary rollout capability                                │
│   Must be able to fallback if issues arise                      │
│                                                                 │
│   Options Considered:                                           │
│                                                                 │
│   1. If/Else Logic                                              │
│      + Simple                                                   │
│      - Tightly coupled                                          │
│      - Hard to test independently                               │
│      - Adding new strategies requires modification              │
│                                                                 │
│   2. Feature Flags Only                                         │
│      + Runtime control                                          │
│      - Doesn't solve code organization                          │
│      - Still have coupling                                      │
│                                                                 │
│   3. Strategy Pattern with Spring                               │
│      + Open/Closed Principle                                    │
│      + Each strategy isolated                                   │
│      + Easy to test                                             │
│      + Spring auto-wires List<Strategy>                         │
│      + @Order for priority                                      │
│                                                                 │
│   Decision: Strategy Pattern                                    │
│                                                                 │
│   Rationale:                                                    │
│   - Clean separation of concerns                                │
│   - Easy to add new strategies                                  │
│   - Each strategy can have its own configuration                │
│   - Natural fit for A/B testing                                 │
│   - Spring makes implementation trivial                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Database Schema

### Core Tables

```
┌────────────────────────────────────────────────────────────────────────┐
│                           ENTITY RELATIONSHIP DIAGRAM                  │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌───────────────────┐         ┌───────────────────┐                  │
│   │    user_account   │         │  customer_profile │                  │
│   ├───────────────────┤         ├───────────────────┤                  │
│   │ id (PK)           │◄────────│ user_id (FK)      │                  │
│   │ username          │    1:1  │ customer_tier_id  │──┐               │
│   │ email             │         │ preferred_lang_id │  │               │
│   │ full_name         │         └───────────────────┘  │               │
│   │ role              │                                │               │
│   └────────┬──────────┘                                │               │
│            │                                           │               │
│            │ 1:N                                       │               │
│            ▼                                           │               │
│   ┌───────────────────┐         ┌───────────────────┐  │               │
│   │   support_ticket  │────────>│  ticket_message   │  │               │
│   ├───────────────────┤    1:N  ├───────────────────┤  │               │
│   │ id (PK)           │         │ id (PK)           │  │               │
│   │ ticket_no         │         │ ticket_id (FK)    │  │               │
│   │ subject           │         │ author_id (FK)    │  │               │
│   │ status            │         │ message_kind      │  │               │
│   │ current_category  │         │ content           │  │               │
│   │ current_priority  │         │ visible_to_cust   │  │               │
│   │ assigned_queue    │         └───────────────────┘  │               │
│   │ customer_id (FK)  │◄───────────────────────────────┘               │
│   │ assigned_agent_id │                                                │
│   │ requires_human_   │         ┌───────────────────┐                  │
│   │ review            │────────>│  ticket_routing   │                  │
│   │ autonomous_meta   │    1:N  ├───────────────────┤                  │
│   │ (JSONB)           │         │ id (PK)           │                  │
│   └────────┬──────────┘         │ ticket_id (FK)    │                  │
│            │                    │ version           │                  │
│            │ 1:N                │ category          │                  │
│            ▼                    │ priority          │                  │
│   ┌───────────────────┐         │ queue             │                  │
│   │ agent_runtime_run │         │ next_action       │                  │
│   ├───────────────────┤         │ confidence        │                  │
│   │ id (PK)           │         │ overridden        │                  │
│   │ ticket_id (FK)    │         │ override_reason   │                  │
│   │ status            │         └───────────────────┘                  │
│   │ termination_reason│                                                │
│   │ total_steps       │         ┌───────────────────┐                  │
│   │ started_at        │────────>│agent_runtime_step │                  │
│   │ ended_at          │    1:N  ├───────────────────┤                  │
│   │ fallback_used     │         │ id (PK)           │                  │
│   │ error_code        │         │ run_id (FK)       │                  │
│   │ error_message     │         │ step_no           │                  │
│   └───────────────────┘         │ step_type         │                  │
│                                 │ planner_output    │                  │
│                                 │ (JSONB)           │                  │
│   ┌───────────────────┐         │ validated_response│                  │
│   │    llm_output     │         │ (JSONB)           │                  │
│   ├───────────────────┤         │ safety_decision   │                  │
│   │ id (PK)           │         │ (JSONB)           │                  │
│   │ ticket_id (FK)    │         │ tool_result       │                  │
│   │ model_tag         │         │ (JSONB)           │                  │
│   │ prompt            │         │ latency_ms        │                  │
│   │ (TEXT)            │         │ success           │                  │
│   │ raw_response      │         └───────────────────┘                  │
│   │ (JSONB)           │                                                │
│   │ parsed_response   │                                                │
│   │ (JSONB)           │                                                │
│   │ parse_status      │                                                │
│   │ error_message     │                                                │
│   │ latency_ms        │                                                │
│   └───────────────────┘                                                │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### JSONB Fields

The system uses PostgreSQL JSONB for flexible, schema-less data:

| Table                | Column                | Purpose                               |
|----------------------|-----------------------|---------------------------------------|
| `support_ticket`     | `autonomous_metadata` | Tracks questions asked, actions taken |
| `agent_runtime_step` | `planner_output`      | Raw LLM response                      |
| `agent_runtime_step` | `validated_response`  | After schema validation               |
| `agent_runtime_step` | `safety_decision`     | Policy evaluation result              |
| `agent_runtime_step` | `tool_result`         | Action execution outcome              |
| `llm_output`         | `prompt`              | Full prompt sent to LLM               |
| `llm_output`         | `raw_response`        | Raw LLM response                      |
| `llm_output`         | `parsed_response`     | Parsed RouterResponse                 |

---

## Navigation

**Previous:** [01 - Overview](./01-overview.md)  
**Next:** [03 - Routing Flow](./03-routing-flow.md)
