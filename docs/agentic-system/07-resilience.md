# 07 - Resilience

> Error handling, circuit breakers, retries, and fallback strategies.

---

## Table of Contents

1. [Why Resilience?](#1-why-resilience)
2. [Circuit Breaker Pattern](#2-circuit-breaker-pattern)
3. [Retry Pattern](#3-retry-pattern)
4. [Fallback Strategy](#4-fallback-strategy)
5. [Error Handling Flow](#5-error-handling-flow)
6. [Configuration](#6-configuration)

---

## 1. Why Resilience?

### The Problem with LLMs

```
┌─────────────────────────────────────────────────────────────────┐
│                        LLM FAILURE MODES                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   LLM calls can fail in many ways:                              │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Network errors                                     │   │
│   │      • Connection timeout                               │   │
│   │      • Connection refused                               │   │
│   │      • DNS resolution failure                           │   │
│   │                                                         │   │
│   │   2. Rate limiting                                      │   │
│   │      • 429 Too Many Requests                            │   │
│   │      • API quota exceeded                               │   │
│   │                                                         │   │
│   │   3. Server errors                                      │   │
│   │      • 500 Internal Server Error                        │   │
│   │      • 503 Service Unavailable                          │   │
│   │      • 504 Gateway Timeout                              │   │
│   │                                                         │   │
│   │   4. Response errors                                    │   │
│   │      • Invalid JSON                                     │   │
│   │      • Schema validation failure                        │   │
│   │      • Incomplete response                              │   │
│   │                                                         │   │
│   │   5. Performance issues                                 │   │
│   │      • High latency (30+ seconds)                       │   │
│   │      • Slow response streaming                          │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Without resilience: Every failure = broken customer experience│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Resilience Goals

```
┌─────────────────────────────────────────────────────────────────┐
│                        RESILIENCE GOALS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. GRACEFUL DEGRADATION                                       │
│      • When LLM fails, system continues to function             │
│      • Customers can still create tickets                       │
│      • Tickets route to human review queue                      │
│                                                                 │
│   2. AUTOMATIC RECOVERY                                         │
│      • Transient errors are retried automatically               │
│      • Circuit breaker allows recovery                          │
│      • No manual intervention needed                            │
│                                                                 │
│   3. PROTECTION                                                 │
│      • Prevent cascade failures                                 │
│      • Protect LLM from overload                                │
│      • Protect database from retry storms                       │
│                                                                 │
│   4. OBSERVABILITY                                              │
│      • Know when failures happen                                │
│      • Track failure rates                                      │
│      • Alert on degradation                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Circuit Breaker Pattern

### What Is Circuit Breaker?

```
┌─────────────────────────────────────────────────────────────────┐
│                    CIRCUIT BREAKER STATES                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                      ┌─────────────┐                            │
│                      │   CLOSED    │                            │
│                      │             │                            │
│                      │ All calls   │                            │
│                      │ go through  │                            │
│                      └──────┬──────┘                            │
│                             │                                   │
│                  Failure rate │                                 │
│                  > threshold  │                                 │
│                             │                                   │
│                             ▼                                   │
│                      ┌─────────────┐                            │
│          ┌──────────>│    OPEN     │<──────────┐                │
│          │           │             │           │                │
│          │           │ All calls   │           │                │
│          │           │ fail fast   │           │                │
│          │           └──────┬──────┘           │                │
│          │                  │                  │                │
│          │         After    │                  │                │
│          │         timeout  │                  │                │
│          │                  │                  │                │
│          │                  ▼                  │                │
│          │           ┌─────────────┐           │                │
│          │           │ HALF_OPEN   │           │                │
│          │           │             │           │                │
│          │           │ Test calls  │           │                │
│          │           │ allowed     │           │                │
│          │           └──────┬──────┘           │                │
│          │                  │                  │                │
│          │     Success      │     Failure      │                │
│          └──────────────────┘──────────────────┘                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why Use Circuit Breaker?

```
┌─────────────────────────────────────────────────────────────────┐
│                    CIRCUIT BREAKER BENEFITS                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   WITHOUT Circuit Breaker:                                      │
│   ────────────────────────                                      │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Ticket 1 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   Ticket 2 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   Ticket 3 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   Ticket 4 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   ...                                                   │   │
│   │                                                         │   │
│   │   Every request waits 30 seconds before failing         │   │
│   │   Thread pool exhausted                                 │   │
│   │   System appears hung                                   │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   WITH Circuit Breaker:                                         │
│   ─────────────────────                                         │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Ticket 1 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   Ticket 2 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   Ticket 3 ──→ LLM (30s timeout) ──→ FAIL               │   │
│   │   [Circuit opens after 50% failure rate]                │   │
│   │   Ticket 4 ──→ Circuit (instant) ──→ FALLBACK           │   │
│   │   Ticket 5 ──→ Circuit (instant) ──→ FALLBACK           │   │
│   │   ...                                                   │   │
│   │                                                         │   │
│   │   After circuit opens: instant fallback                 │   │
│   │   No thread exhaustion                                  │   │
│   │   System stays responsive                               │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Retry Pattern

### Retry with Exponential Backoff

```
┌─────────────────────────────────────────────────────────────────┐
│                    RETRY WITH BACKOFF                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Without Backoff:                                              │
│   ──────────────────                                            │
│                                                                 │
│   Attempt 1 ──→ FAIL (immediately)                              │
│   Attempt 2 ──→ FAIL (immediately)                              │
│   Attempt 3 ──→ FAIL (immediately)                              │
│                                                                 │
│   Problem: All retries hit server at same time                  │
│   Result: Thundering herd, more failures                        │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   With Exponential Backoff:                                     │
│   ─────────────────────────                                     │
│                                                                 │
│   Attempt 1 ──→ FAIL                                            │
│   Wait 1s                                                       │
│   Attempt 2 ──→ FAIL                                            │
│   Wait 2s                                                       │
│   Attempt 3 ──→ SUCCESS                                         │
│                                                                 │
│   Benefit: Gives server time to recover                         │
│   Result: Higher success rate on retries                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Retry Configuration

```
┌─────────────────────────────────────────────────────────────────┐
│                    RETRY CONFIGURATION                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   resilience4j.retry.instances.llmRetry:                        │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   max-attempts: 3                                       │   │
│   │   │                                                     │   │
│   │   │  Total attempts = 1 initial + 2 retries             │   │
│   │   │                                                     │   │
│   │   wait-duration: 1s                                     │   │
│   │   │                                                     │   │
│   │   │  Initial wait before first retry                    │   │
│   │   │                                                     │   │
│   │   enable-exponential-backoff: true                      │   │
│   │   │                                                     │   │
│   │   │  Each retry waits longer than previous              │   │
│   │   │                                                     │   │
│   │   exponential-backoff-multiplier: 2                     │   │
│   │   │                                                     │   │
│   │   │  1s → 2s → 4s → ...                                 │   │
│   │   │                                                     │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Retry Timeline:                                               │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Attempt 1: t=0      ──→ FAIL                          │   │
│   │   Wait: 1s                                              │   │
│   │   Attempt 2: t=1s     ──→ FAIL                          │   │
│   │   Wait: 2s                                              │   │
│   │   Attempt 3: t=3s     ──→ SUCCESS or FALLBACK           │   │
│   │                                                         │   │
│   │   Total time: ~3s before fallback                       │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Fallback Strategy

### The Safe Fallback

```
┌─────────────────────────────────────────────────────────────────┐
│                        FALLBACK STRATEGY                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   When all else fails, route to human review:                   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   routingFallback(request, ticketId, error):            │   │
│   │                                                         │   │
│   │       log.error("Routing failed, using fallback");      │   │
│   │                                                         │   │
│   │       // Log the failure for analysis                   │   │
│   │       llmOutputService.persistFailure(                  │   │
│   │           ticketId, error.getMessage()                  │   │
│   │       );                                                │   │
│   │                                                         │   │
│   │       // Return safe default                            │   │
│   │       return RouterResponse.builder()                   │   │
│   │           .category(TicketCategory.OTHER)               │   │
│   │           .priority(TicketPriority.MEDIUM)              │   │
│   │           .queue(TicketQueue.GENERAL_Q)                 │   │
│   │           .nextAction(NextAction.HUMAN_REVIEW) // ← Key │   │
│   │           .confidence(BigDecimal.ZERO)                  │   │
│   │           .rationaleTags(List.of("MODEL_ERROR"))        │   │
│   │           .build();                                     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Why HUMAN_REVIEW?                                             │
│   ──────────────────                                            │
│   • Safest possible action                                      │
│   • Human will make the right decision                          │
│   • No risk to customer                                         │
│   • Can still be processed                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Fallback Triggers

```
┌─────────────────────────────────────────────────────────────────┐
│                        FALLBACK TRIGGERS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Fallback is triggered when:                                   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   1. Circuit breaker is OPEN                            │   │
│   │      → LLM has been failing too much                    │   │
│   │      → Stop trying, use fallback immediately            │   │
│   │                                                         │   │
│   │   2. All retries exhausted                              │   │
│   │      → Tried 3 times, all failed                        │   │
│   │      → Give up, use fallback                            │   │
│   │                                                         │   │
│   │   3. Timeout exceeded                                   │   │
│   │      → LLM took too long                                │   │
│   │      → Abort and use fallback                           │   │
│   │                                                         │   │
│   │   4. Non-retryable error                                │   │
│   │      → 401 Unauthorized                                 │   │
│   │      → 403 Forbidden                                    │   │
│   │      → Don't retry, use fallback immediately            │   │
│   │                                                         │   │
│   │   5. Schema validation failure (after repair attempts)  │   │
│   │      → LLM output couldn't be parsed                    │   │
│   │      → Use fallback with HUMAN_REVIEW                   │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Error Handling Flow

### Complete Error Handling Diagram

```
                              LLM Call
                                 │
                                 ▼
              ┌─────────────────────────────────────┐
              │         Success?                    │
              └─────────────────┬───────────────────┘
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
                YES                           NO
                 │                             │
                 ▼                             ▼
          ┌──────────┐         ┌──────────────────────────────┐
          │ Return   │         │ Check Circuit Breaker State  │
          │ Response │         └───────────────┬──────────────┘
          └──────────┘                         │
                                    ┌──────────┴──────────┐
                                    │                     │
                                  OPEN                  CLOSED/HALF_OPEN
                                    │                     │
                                    ▼                     ▼
                         ┌──────────────┐    ┌─────────────────────┐
                         │   FALLBACK   │    │ Check Retry Count   │
                         │   Immediately│    └───────────┬─────────┘
                         └──────────────┘                │
                                               ┌──────────┴──────────┐
                                               │                     │
                                         Max Retries              More Available
                                         Reached                     │
                                               │                     │
                                               ▼                     ▼
                                    ┌──────────────┐    ┌─────────────────────┐
                                    │   FALLBACK   │    │ Wait (backoff)      │
                                    └──────────────┘    │ Then retry          │
                                                        └──────────┬──────────┘
                                                                   │
                                                                   ▼
                                                           [Back to LLM Call]
```

---

## 6. Configuration

### Complete Resilience4j Configuration

```properties
# === Circuit Breaker ===
resilience4j.circuitbreaker.instances.llmCircuit.sliding-window-size=10
resilience4j.circuitbreaker.instances.llmCircuit.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.llmCircuit.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.llmCircuit.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.llmCircuit.slow-call-rate-threshold=50
resilience4j.circuitbreaker.instances.llmCircuit.slow-call-duration-threshold=10s
# === Retry ===
resilience4j.retry.instances.llmRetry.max-attempts=3
resilience4j.retry.instances.llmRetry.wait-duration=1s
resilience4j.retry.instances.llmRetry.enable-exponential-backoff=true
resilience4j.retry.instances.llmRetry.exponential-backoff-multiplier=2
# === Time Limiter ===
resilience4j.timelimiter.instances.llmTimeout.timeout-duration=30s
resilience4j.timelimiter.instances.llmTimeout.cancel-running-future=true
```

### Configuration Explained

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONFIGURATION EXPLAINED                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Circuit Breaker:                                              │
│   ─────────────────                                             │
│   sliding-window-size=10                                        │
│   → Consider last 10 calls for failure rate calculation         │
│                                                                 │
│   failure-rate-threshold=50                                     │
│   → Open circuit if > 50% of calls fail                         │
│                                                                 │
│   wait-duration-in-open-state=30s                               │
│   → Wait 30 seconds before trying again (half-open)             │
│                                                                 │
│   permitted-number-of-calls-in-half-open-state=3                │
│   → Try 3 calls in half-open before deciding state              │
│                                                                 │
│   slow-call-rate-threshold=50                                   │
│   → Also open if > 50% of calls are slow                        │
│                                                                 │
│   slow-call-duration-threshold=10s                              │
│   → Calls taking > 10s are considered slow                      │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Retry:                                                        │
│   ──────                                                        │
│   max-attempts=3                                                │
│   → Try up to 3 times (1 initial + 2 retries)                   │
│                                                                 │
│   wait-duration=1s                                              │
│   → Wait 1 second before first retry                            │
│                                                                 │
│   exponential-backoff-multiplier=2                              │
│   → Double wait time each retry: 1s → 2s → 4s                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [06 - Policy Engine](./06-policy-engine.md)  
**Next:** [08 - Observability](./08-observability.md)
