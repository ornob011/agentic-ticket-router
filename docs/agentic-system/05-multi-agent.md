# 05 - Multi-Agent Orchestration

> Understanding the supervisor-specialist delegation pattern.

---

## Table of Contents

1. [Agent Roles Overview](#1-agent-roles-overview)
2. [Orchestration Flow](#2-orchestration-flow)
3. [Delegation Decision](#3-delegation-decision)
4. [Orchestration Modes](#4-orchestration-modes)
5. [When to Use Each Mode](#5-when-to-use-each-mode)

---

## 1. Agent Roles Overview

### Role Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                      AGENT ROLE HIERARCHY                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                       SUPERVISOR                        │   │
│   │                      (Orchestrator)                     │   │
│   │                                                         │   │
│   │   • First LLM call always uses SUPERVISOR role          │   │
│   │   • Analyzes ticket and decides routing strategy        │   │
│   │   • Can delegate to specialists based on task type      │   │
│   │   • Makes final routing decisions                       │   │
│   └────────────────────────┬────────────────────────────────┘   │
│                            │                                    │
│            ┌───────────────┼───────────────┐                    │
│            │               │               │                    │
│            ▼               ▼               ▼                    │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│   │ CLASSIFIER  │  │  RESOLVER   │  │     QA      │             │
│   │ (Specialist)│  │ (Specialist)│  │ (Specialist)│             │
│   ├─────────────┤  ├─────────────┤  ├─────────────┤             │
│   │ Categorizes │  │ Attempts    │  │ Asks        │             │
│   │ and         │  │ automatic   │  │ clarifying  │             │
│   │ prioritizes │  │ resolution  │  │ questions   │             │
│   └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│            ┌───────────────────────────────────────┐            │
│            │                                       │            │
│            ▼                                       │            │
│   ┌─────────────┐                                  │            │
│   │ ESCALATOR   │                                  │            │
│   │ (Specialist)│                                  │            │
│   ├─────────────┤                                  │            │
│   │ Handles     │                                  │            │
│   │ escalations │                                  │            │
│   │ and human   │                                  │            │
│   │ review      │                                  │            │
│   └─────────────┘                                  │            │
│                                                    │            │
└─────────────────────────────────────────────────────────────────┘
```

### Role Details

```
┌─────────────────────────────────────────────────────────────────┐
│                    ROLE DEFINITIONS                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   SUPERVISOR (Orchestrator)                                     │
│   ─────────────────────────                                     │
│   Type: ORCHESTRATOR                                            │
│   Description: Routes tickets to specialist agents              │
│   Capabilities:                                                 │
│   • canRoute() = true (makes routing decisions)                 │
│   • canExecuteTools() = false                                   │
│   Handles: Initial analysis and delegation                      │
│                                                                 │
│   CLASSIFIER (Specialist)                                       │
│   ────────────────────────                                      │
│   Type: SPECIALIST                                              │
│   Description: Categorizes and prioritizes incoming tickets     │
│   Capabilities:                                                 │
│   • canRoute() = false                                          │
│   • canExecuteTools() = false                                   │
│   Handles: ASSIGN_QUEUE, CHANGE_PRIORITY                        │
│                                                                 │
│   RESOLVER (Specialist)                                         │
│   ────────────────────────                                      │
│   Type: SPECIALIST                                              │
│   Description: Attempts automatic resolution                    │
│   Capabilities:                                                 │
│   • canRoute() = false                                          │
│   • canExecuteTools() = true (can execute resolution tools)     │
│   Handles: AUTO_REPLY, USE_KNOWLEDGE_ARTICLE, USE_TEMPLATE,     │
│            UPDATE_CUSTOMER_PROFILE, AUTO_RESOLVE                │
│                                                                 │
│   QA (Specialist)                                               │
│   ────────────────                                              │
│   Type: SPECIALIST                                              │
│   Description: Asks clarifying questions                        │
│   Capabilities:                                                 │
│   • canRoute() = false                                          │
│   • canExecuteTools() = true                                    │
│   Handles: ASK_CLARIFYING                                       │
│                                                                 │
│   ESCALATOR (Specialist)                                        │
│   ─────────────────────────                                     │
│   Type: SPECIALIST                                              │
│   Description: Handles escalation and human review              │
│   Capabilities:                                                 │
│   • canRoute() = false                                          │
│   • canExecuteTools() = false                                   │
│   Handles: HUMAN_REVIEW, ESCALATE, AUTO_ESCALATE                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Orchestration Flow

### Single-Agent vs Multi-Agent

```
┌─────────────────────────────────────────────────────────────────┐
│                        SINGLE-AGENT MODE                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   SUPERVISOR                                            │   │
│   │                                                         │   │
│   │   ┌─────────────────────────────────────────────────┐   │   │
│   │   │ One LLM call handles everything:                │   │   │
│   │   │                                                 │   │   │
│   │   │ • Analyze ticket                                │   │   │
│   │   │ • Determine category                            │   │   │
│   │   │ • Determine priority                            │   │   │
│   │   │ • Determine queue                               │   │   │
│   │   │ • Determine action                              │   │   │
│   │   │ • Generate any content (draft, question)        │   │   │
│   │   │                                                 │   │   │
│   │   └─────────────────────────────────────────────────┘   │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Pros:                                                         │
│   • Faster (single LLM call)                                    │
│   • Simpler to debug                                            │
│   • Lower cost                                                  │
│                                                                 │
│   Cons:                                                         │
│   • Jack of all trades, master of none                          │
│   • May miss nuanced decisions                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        MULTI-AGENT MODE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────┐                                               │
│   │  SUPERVISOR │                                               │
│   │             │                                               │
│   │ Analyze     │                                               │
│   │ ticket,     │                                               │
│   │ decide      │                                               │
│   │ specialist  │                                               │
│   └──────┬──────┘                                               │
│          │                                                      │
│          │ targetRole = RESOLVER                                │
│          │ handoff = true                                       │
│          ▼                                                      │
│   ┌─────────────┐                                               │
│   │  RESOLVER   │                                               │
│   │             │                                               │
│   │ Generate    │                                               │
│   │ draft reply │                                               │
│   │ using KB    │                                               │
│   └─────────────┘                                               │
│                                                                 │
│   Pros:                                                         │
│   • Specialists are better at specific tasks                    │
│   • More nuanced decisions                                      │
│   • Can use role-specific prompts                               │
│                                                                 │
│   Cons:                                                         │
│   • Slower (multiple LLM calls)                                 │
│   • Higher cost                                                 │
│   • More complex to debug                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Delegation Decision

### How Supervisor Decides to Delegate

```
┌─────────────────────────────────────────────────────────────────┐
│                        DELEGATION LOGIC                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Step 1: SUPERVISOR makes initial decision                     │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   LLM Response includes:                                │   │
│   │   {                                                     │   │
│   │     "next_action": "AUTO_REPLY",                        │   │
│   │     "category": "BILLING",                              │   │
│   │     "priority": "HIGH",                                 │   │
│   │     "queue": "BILLING_Q",                               │   │
│   │     "draft_reply": null,  // May be null initially      │   │
│   │     ...                                                 │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│   Step 2: Determine target role based on action                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   resolveTargetRole(actorRole, response):               │   │
│   │                                                         │   │
│   │     if (actorRole != SUPERVISOR) return actorRole;      │   │
│   │                                                         │   │
│   │     switch (nextAction) {                               │   │
│   │       ASK_CLARIFYING          → QA                      │   │
│   │       HUMAN_REVIEW            → ESCALATOR               │   │
│   │       ESCALATE                → ESCALATOR               │   │
│   │       AUTO_ESCALATE           → ESCALATOR               │   │
│   │       UPDATE_CUSTOMER_PROFILE → RESOLVER                │   │
│   │       USE_KNOWLEDGE_ARTICLE   → RESOLVER                │   │
│   │       USE_TEMPLATE            → RESOLVER                │   │
│   │       AUTO_REPLY              → RESOLVER                │   │
│   │       AUTO_RESOLVE            → RESOLVER                │   │
│   │       ASSIGN_QUEUE            → CLASSIFIER              │   │
│   │       CHANGE_PRIORITY         │ CLASSIFIER              │   │
│   │       ADD_INTERNAL_NOTE       │ CLASSIFIER              │   │
│   │       TRIGGER_NOTIFICATION    │ CLASSIFIER              │   │
│   │       REOPEN_TICKET           → CLASSIFIER              │   │
│   │     }                                                   │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│   Step 3: Check if delegation should happen                     │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   shouldDelegate = (                                    │   │
│   │       mode == MULTI_AGENT &&                            │   │
│   │       targetRole != SUPERVISOR                          │   │
│   │   )                                                     │   │
│   │                                                         │   │
│   │   if (shouldDelegate) {                                 │   │
│   │       // Make second LLM call with specialist prompt    │   │
│   │       specialistResponse = llm.decideForRole(           │   │
│   │           request, ticketId, targetRole                 │   │
│   │       );                                                │   │
│   │                                                         │   │
│   │       state.setHandoff(true);                           │   │
│   │       state.setHandoffReason("Supervisor delegation");  │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Action-to-Role Mapping Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    ACTION → ROLE MAPPING                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   NextAction                 Target Role         Category       │
│   ══════════════════════════════════════════════════════════    │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                    ESCALATOR                            │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │  HUMAN_REVIEW         → Human review required           │   │
│   │  ESCALATE             → Supervisor escalation           │   │
│   │  AUTO_ESCALATE        → System-triggered escalation     │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                      QA                                 │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │  ASK_CLARIFYING       → Need more information           │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                    RESOLVER                             │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │  AUTO_REPLY           → Generate response               │   │
│   │  AUTO_RESOLVE         → Close with resolution           │   │
│   │  USE_KNOWLEDGE_ARTICLE → Apply KB solution              │   │
│   │  USE_TEMPLATE         → Apply response template         │   │
│   │  UPDATE_CUSTOMER_PROFILE → Update customer data         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                   CLASSIFIER                            │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │  ASSIGN_QUEUE         → Route to team                   │   │
│   │  CHANGE_PRIORITY      → Adjust urgency                  │   │
│   │  ADD_INTERNAL_NOTE    → Document context                │   │
│   │  TRIGGER_NOTIFICATION → Alert stakeholders              │   │
│   │  REOPEN_TICKET        → Reopen closed issue             │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Orchestration Modes

### Mode Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│                       ORCHESTRATION MODES                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   SINGLE_AGENT                                                  │
│   ════════════                                                  │
│   Configuration: orchestration-mode=SINGLE_AGENT                │
│                                                                 │
│   Flow:                                                         │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   SUPERVISOR ──────────────────────────────────────────>│   │
│   │       │                                                 │   │
│   │       │ (makes all decisions in one call)               │   │
│   │       │                                                 │   │
│   │       └────────────────────────────────────────────────>│   │
│   │                                 Result                  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   LLM Calls: 1                                                  │
│   Latency: Lower                                                │
│   Cost: Lower                                                   │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   MULTI_AGENT                                                   │
│   ═══════════                                                   │
│   Configuration: orchestration-mode=MULTI_AGENT                 │
│                                                                 │
│   Flow:                                                         │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   SUPERVISOR ──────────────────────────────────────────>│   │
│   │       │                                                 │   │
│   │       │ targetRole = RESOLVER                           │   │
│   │       │ handoff = true                                  │   │
│   │       ▼                                                 │   │
│   │   RESOLVER ────────────────────────────────────────────>│   │
│   │       │                                                 │   │
│   │       │ (specialist generates content)                  │   │
│   │       │                                                 │   │
│   │       └────────────────────────────────────────────────>│   │
│   │                                 Result                  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   LLM Calls: 2 (Supervisor + Specialist)                        │
│   Latency: Higher                                               │
│   Cost: Higher                                                  │
│   Quality: Potentially better for complex tasks                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Configuration

```properties
# Single agent (default, faster)
agent.runtime.orchestration-mode=SINGLE_AGENT
# Multi-agent (slower, potentially better quality)
agent.runtime.orchestration-mode=MULTI_AGENT
```

---

## 5. When to Use Each Mode

### Decision Guide

```
┌─────────────────────────────────────────────────────────────────┐
│                    MODE SELECTION GUIDE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Use SINGLE_AGENT when:                                        │
│   ─────────────────────────                                     │
│   • Speed is critical (real-time responses)                     │
│   • Cost is a concern (fewer LLM calls)                         │
│   • Tasks are relatively simple                                 │
│   • You're using a capable model (GPT-4, Claude 3.5)            │
│   • Initial rollout/testing phase                               │
│                                                                 │
│   Use MULTI_AGENT when:                                         │
│   ─────────────────────────                                     │
│   • Quality is more important than speed                        │
│   • Tasks benefit from specialization                           │
│   • Complex reasoning required                                  │
│   • Content generation is important (drafts, questions)         │
│   • You want role-specific prompts                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Example Scenarios

```
┌─────────────────────────────────────────────────────────────────┐
│                    SCENARIO EXAMPLES                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Scenario 1: Simple Routing                                    │
│   ─────────────────────────────                                 │
│   Input: "How do I change my password?"                         │
│   Action: ASSIGN_QUEUE → SECURITY_Q                             │
│   Recommendation: SINGLE_AGENT                                  │
│   Reasoning: Simple categorization, no content generation       │
│                                                                 │
│   ───────────────────────────────────────────────────────────   │
│                                                                 │
│   Scenario 2: Draft Response                                    │
│   ─────────────────────────────                                 │
│   Input: "My internet is slow, I've tried restarting..."        │
│   Action: AUTO_REPLY (needs quality response)                   │
│   Recommendation: MULTI_AGENT                                   │
│   Reasoning: Specialist can generate better response with       │
│              access to KB articles and troubleshooting steps    │
│                                                                 │
│   ───────────────────────────────────────────────────────────   │
│                                                                 │
│   Scenario 3: Security Escalation                               │
│   ─────────────────────────────                                 │
│   Input: "I think my account was hacked"                        │
│   Action: ESCALATE → ESCALATOR                                  │
│   Recommendation: SINGLE_AGENT (or MULTI_AGENT for notes)       │
│   Reasoning: Escalation is straightforward, specialist adds     │
│              detailed internal notes for security team          │
│                                                                 │
│   ───────────────────────────────────────────────────────────   │
│                                                                 │
│   Scenario 4: Clarifying Questions                              │
│   ─────────────────────────────                                 │
│   Input: "It's not working"                                     │
│   Action: ASK_CLARIFYING → QA                                   │
│   Recommendation: MULTI_AGENT                                   │
│   Reasoning: QA specialist is trained to ask helpful questions  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [04 - State Machine](./04-state-machine.md)  
**Next:** [06 - Policy Engine](./06-policy-engine.md)
