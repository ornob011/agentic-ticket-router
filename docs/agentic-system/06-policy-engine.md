# 06 - Policy Engine

> Understanding the guardrails and safety rules that govern AI decisions.

---

## Table of Contents

1. [Why Policy Engine?](#1-why-policy-engine)
2. [Rule Execution Model](#2-rule-execution-model)
3. [Built-in Rules](#3-built-in-rules)
4. [Rule Decision Tree](#4-rule-decision-tree)
5. [Adding Custom Rules](#5-adding-custom-rules)
6. [Policy Configuration](#6-policy-configuration)

---

## 1. Why Policy Engine?

### The Problem

```
┌─────────────────────────────────────────────────────────────────┐
│                 THE PROBLEM WITH RAW LLM OUTPUT                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   LLM Output:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ {                                                       │   │
│   │   "category": "BILLING",                                │   │
│   │   "priority": "CRITICAL",                               │   │
│   │   "queue": "GENERAL_Q",                                 │   │
│   │   "next_action": "AUTO_RESOLVE",                        │   │
│   │   "confidence": 0.45,  // Low confidence                │   │
│   │   "draft_reply": "Try restarting your router..."        │   │
│   │ }                                                       │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Issues:                                                       │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   ❌ Low confidence (0.45) but action is AUTO_RESOLVE    │   │
│   │   ❌ CRITICAL priority shouldn't auto-resolve            │   │
│   │   ❌ Category is BILLING but queue is GENERAL_Q          │   │
│   │   ❌ Draft reply is for TECH issue, not BILLING          │   │
│   │                                                         │   │
│   │   Without policy engine: This would be sent to customer!│   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### The Solution

```
┌─────────────────────────────────────────────────────────────────┐
│                     POLICY ENGINE SOLUTION                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   LLM Output  ──────→  Policy Engine  ──────→  Safe Output      │
│                                                                 │
│   ┌─────────────────┐   ┌─────────────────┐   ┌──────────────┐  │
│   │ AUTO_RESOLVE    │   │ Rule 1: Check   │   │ HUMAN_REVIEW │  │
│   │ confidence: 0.45│ → │   confidence    │ → │ confidence: 0│  │
│   │ priority:       │   │                 │   │ overridden   │  │
│   │   CRITICAL      │   │ Rule 2: Check   │   │ true         │  │
│   │                 │   │   CRITICAL      │   │              │  │
│   └─────────────────┘   └─────────────────┘   └──────────────┘  │
│                                                                 │
│   The policy engine:                                            │
│   • Validates LLM decisions                                     │
│   • Enforces business rules                                     │
│   • Overrides dangerous actions                                 │
│   • Forces human review when uncertain                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Rule Execution Model

### Execution Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                       POLICY ENGINE FLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Input: RouterResponse (from LLM)                              │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   for (RouterPolicyRule rule : policyRules) {           │   │
│   │                                                         │   │
│   │       // Rules are ordered by @Order annotation         │   │
│   │                                                         │   │
│   │       PolicyRuleOutcome outcome = rule.apply(state);    │   │
│   │                                                         │   │
│   │       if (outcome.changed()) {                          │   │
│   │           state = outcome.state();                      │   │
│   │           // Continue to next rule with modified state  │   │
│   │       }                                                 │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Output: RouterResponse (possibly modified)                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule Interface

```
┌─────────────────────────────────────────────────────────────────┐
│                    RULE INTERFACE                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   interface RouterPolicyRule {                                  │
│                                                                 │
│       // Unique identifier for this rule                        │
│       RoutingPolicyRuleCode code();                             │
│                                                                 │
│       // Apply the rule to the current state                    │
│       PolicyRuleOutcome apply(PolicyEvaluationState state);     │
│   }                                                             │
│                                                                 │
│   ────────────────────────────────────────────────────────────  │
│                                                                 │
│   class PolicyRuleOutcome {                                     │
│                                                                 │
│       // Whether this rule changed the response                 │
│       boolean changed();                                        │
│                                                                 │
│       // The (possibly modified) state                          │
│       PolicyEvaluationState state();                            │
│                                                                 │
│       // Factory methods                                        │
│       static PolicyRuleOutcome unchanged(state);                │
│       static PolicyRuleOutcome changed(state);                  │
│   }                                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Built-in Rules

### Rule Order and Priority

```
┌─────────────────────────────────────────────────────────────────┐
│                    BUILT-IN RULES                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Order    Rule                        Trigger Condition        │
│   ═══════════════════════════════════════════════════════════   │
│                                                                 │
│   1        SecurityContent            Category = SECURITY       │
│            EscalationRule             OR dangerous tags         │
│                                                                 │
│   2        CriticalMinConfidence      Priority = CRITICAL       │
│            Rule                       AND confidence < 0.90     │
│                                                                 │
│   3        AutoRouteThreshold         confidence < 0.75         │
│            Rule                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule 1: SecurityContentEscalationRule

```
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY ESCALATION RULE                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Purpose: Ensure security issues are always escalated          │
│   Order: 1 (highest priority)                                   │
│                                                                 │
│   Trigger Conditions:                                           │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Category = SECURITY                                   │   │
│   │   OR                                                    │   │
│   │   rationaleTags contains:                               │   │
│   │     - "ACCOUNT_ACCESS"                                  │   │
│   │     - "UNAUTHORIZED"                                    │   │
│   │     - "DATA_BREACH"                                     │   │
│   │     - "PHISHING"                                        │   │
│   │     - etc.                                              │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Overrides:                                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   queue      → SECURITY_Q                               │   │
│   │   priority   → HIGH                                     │   │
│   │   nextAction → ESCALATE                                 │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Why first?                                                    │
│   • Security issues must always be handled by security team     │
│   • Overrides any other decision (even if high confidence)      │
│   • Prevents AI from auto-resolving security issues             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule 2: CriticalMinConfidenceRule

```
┌─────────────────────────────────────────────────────────────────┐
│                CRITICAL MINIMUM CONFIDENCE RULE                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Purpose: Require high confidence for CRITICAL tickets         │
│   Order: 2                                                      │
│                                                                 │
│   Trigger Conditions:                                           │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Priority = CRITICAL                                   │   │
│   │   AND                                                   │   │
│   │   confidence < CRITICAL_MIN_CONF (default: 0.90)        │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Overrides:                                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   nextAction → HUMAN_REVIEW                             │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Why?                                                          │
│   • CRITICAL tickets need human attention                       │
│   • Low confidence = uncertain decision                         │
│   • Better safe than sorry for critical issues                  │
│                                                                 │
│   Example:                                                      │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Input: priority=CRITICAL, confidence=0.75             │   │
│   │                                                         │   │
│   │   CRITICAL_MIN_CONF = 0.90                              │   │
│   │   0.75 < 0.90 → TRIGGERED                               │   │
│   │                                                         │   │
│   │   Output: nextAction = HUMAN_REVIEW                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rule 3: AutoRouteThresholdRule

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTO ROUTE THRESHOLD RULE                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Purpose: Require minimum confidence for autonomous actions    │
│   Order: 3 (lowest priority)                                    │
│                                                                 │
│   Trigger Conditions:                                           │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   confidence < AUTO_ROUTE_THRESHOLD (default: 0.75)     │   │
│   │   AND                                                   │   │
│   │   No previous rule triggered (policyTriggered = false)  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Overrides:                                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   nextAction → HUMAN_REVIEW                             │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Why?                                                          │
│   • Low confidence = AI is uncertain                            │
│   • Better to involve human than make mistakes                  │
│   • Prevents bad customer experiences                           │
│                                                                 │
│   Example:                                                      │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Input: confidence=0.60                                │   │
│   │                                                         │   │
│   │   AUTO_ROUTE_THRESHOLD = 0.75                           │   │
│   │   0.60 < 0.75 → TRIGGERED                               │   │
│   │                                                         │   │
│   │   Output: nextAction = HUMAN_REVIEW                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Rule Decision Tree

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
               ┌──────────────┴──────────────┐
               │                             │
              YES                           NO
               │                             │
               ▼                             ▼
    ┌──────────────────────┐    ┌─────────────────────────┐
    │ Override:            │    │ Rule 2: CriticalMinConf │
    │ queue → SECURITY_Q   │    │                         │
    │ priority → HIGH      │    │ Priority = CRITICAL     │
    │ action → ESCALATE    │    │ AND conf < 0.90?        │
    │                      │    └───────────┬─────────────┘
    │ policyTriggered=true │                │
    └──────────────────────┘    ┌───────────┴───────────┐
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

---

## 5. Adding Custom Rules

### Step-by-Step Guide

```
┌─────────────────────────────────────────────────────────────────┐
│                      ADDING A CUSTOM RULE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Example: VIP Customer Rule                                    │
│   ──────────────────────────                                    │
│   Require human review for VIP customers with low confidence    │
│                                                                 │
│   Step 1: Create the rule class                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   @Component                                            │   │
│   │   @Order(4)  // After existing rules                    │   │
│   │   @RequiredArgsConstructor                              │   │
│   │   public class VipCustomerRule implements Rule {        │   │
│   │                                                         │   │
│   │       private final PolicyValueLookupService service;   │   │
│   │       private final RouterResponseEditor editor;        │   │
│   │                                                         │   │
│   │       @Override                                         │   │
│   │       public RoutingPolicyRuleCode code() {             │   │
│   │           return RoutingPolicyRuleCode.VIP_CUSTOMER;    │   │
│   │       }                                                 │   │
│   │                                                         │   │
│   │       @Override                                         │   │
│   │       public PolicyRuleOutcome apply(State state) {     │   │
│   │           RouterResponse response = state.response();   │   │
│   │                                                         │   │
│   │           if (state.policyTriggered())                  │   │
│   │               return PolicyRuleOutcome.unchanged(state);│   │
│   │                                                         │   │
│   │           if (!isVipCustomer(state))                    │   │
│   │               return PolicyRuleOutcome.unchanged(state);│   │
│   │                                                         │   │
│   │           BigDecimal min = service.getValue(            │   │
│   │               PolicyConfigKey.VIP_MIN_CONFIDENCE        │   │
│   │           );                                            │   │
│   │                                                         │   │
│   │           if (response.getConfidence()                  │   │
│   │                   .compareTo(min) >= 0)                 │   │
│   │               return PolicyRuleOutcome.unchanged(state);│   │
│   │                                                         │   │
│   │           RouterResponse updated = editor.mutate(       │   │
│   │               response, b -> b                          │   │
│   │               .nextAction(NextAction.HUMAN_REVIEW)      │   │
│   │           );                                            │   │
│   │                                                         │   │
│   │           return PolicyRuleOutcome.changed(             │   │
│   │               state.withResponseAndTriggered(updated)   │   │
│   │           );                                            │   │
│   │       }                                                 │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Step 2: Add enum value                                        │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   enum RoutingPolicyRuleCode {                          │   │
│   │       SECURITY_CONTENT_ESCALATION,                      │   │
│   │       CRITICAL_MIN_CONF,                                │   │
│   │       AUTO_ROUTE_THRESHOLD,                             │   │
│   │       VIP_CUSTOMER  // Add this                         │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Step 3: Add config key (if needed)                            │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   enum PolicyConfigKey {                                │   │
│   │       AUTO_ROUTE_THRESHOLD,                             │   │
│   │       CRITICAL_MIN_CONF,                                │   │
│   │       VIP_MIN_CONFIDENCE  // Add this                   │   │
│   │   }                                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Step 4: Add database config                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   INSERT INTO policy_config (config_key, config_value)  │   │
│   │   VALUES ('VIP_MIN_CONFIDENCE', 0.95);                  │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Policy Configuration

### Database Configuration

```
┌─────────────────────────────────────────────────────────────────┐
│                       POLICY_CONFIG TABLE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Table: policy_config                                          │
│                                                                 │
│   ┌──────────────────────┬─────────┬────────────────────────┐   │
│   │ config_key           │ value   │ description            │   │
│   ├──────────────────────┼─────────┼────────────────────────┤   │
│   │ AUTO_ROUTE_THRESHOLD │ 0.75    │ Min conf for auto      │   │
│   │                      │         │ routing                │   │
│   ├──────────────────────┼─────────┼────────────────────────┤   │
│   │ CRITICAL_MIN_CONF    │ 0.90    │ Min conf for CRITICAL  │   │
│   │                      │         │ priority tickets       │   │
│   ├──────────────────────┼─────────┼────────────────────────┤   │
│   │ MAX_AUTO_ACTIONS     │ 5       │ Max autonomous actions │   │
│   │                      │         │ per ticket             │   │
│   ├──────────────────────┼─────────┼────────────────────────┤   │
│   │ MAX_QUESTIONS        │ 3       │ Max clarifying quest.  │   │
│   │                      │         │ per ticket             │   │
│   └──────────────────────┴─────────┴────────────────────────┘   │
│                                                                 │
│   These values are cached and can be changed at runtime.        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Updating Configuration

```sql
-- Increase minimum confidence for auto-routing
UPDATE policy_config
SET config_value = 0.80
WHERE config_key = 'AUTO_ROUTE_THRESHOLD';

-- Increase CRITICAL threshold
UPDATE policy_config
SET config_value = 0.95
WHERE config_key = 'CRITICAL_MIN_CONF';
```

---

## Navigation

**Previous:** [05 - Multi-Agent Orchestration](./05-multi-agent.md)  
**Next:** [07 - Resilience](./07-resilience.md)
