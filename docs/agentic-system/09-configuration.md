# 09 - Configuration

> Complete reference for all configuration options.

---

## Table of Contents

1. [Configuration Overview](#1-configuration-overview)
2. [Agent Runtime Settings](#2-agent-runtime-settings)
3. [Policy Thresholds](#3-policy-thresholds)
4. [Resilience Settings](#4-resilience-settings)
5. [Database Settings](#5-database-settings)
6. [Mode Reference](#6-mode-reference)
7. [Anti-Patterns](#7-anti-patterns)

---

## 1. Configuration Overview

### Configuration Sources

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONFIGURATION SOURCES                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Priority (highest first):                                     │
│                                                                 │
│   1. Environment variables                                      │
│      DB_PASSWORD=secret                                         │
│                                                                 │
│   2. Command-line arguments                                     │
│      java -jar app.jar --agent.runtime.enabled=false            │
│                                                                 │
│   3. application.properties                                     │
│      agent.runtime.enabled=true                                 │
│                                                                 │
│   4. Default values in code                                     │
│      @ConfigurationProperties(defaultValue = "true")            │
│                                                                 │
│   5. Database (for policy_config)                               │
│      Runtime-configurable thresholds                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Agent Runtime Settings

### Complete Reference

```properties
# ═══════════════════════════════════════════════════════════════
# AGENT RUNTIME CONFIGURATION
# ═══════════════════════════════════════════════════════════════
# Enable/disable the agent runtime
# When disabled, falls back to ClassicRoutingStrategy
agent.runtime.enabled=true
# Shadow mode - run state machine but don't execute actions
# Useful for testing without affecting production
agent.runtime.shadow-mode=false
# Canary rollout - only route specific queues through agent runtime
# When canary-enabled=true, only queues in allowed-queues use agent runtime
agent.runtime.canary-enabled=false
agent.runtime.allowed-queues=BILLING_Q,TECH_Q
# ═══════════════════════════════════════════════════════════════
# SAFETY LIMITS
# ═══════════════════════════════════════════════════════════════
# Maximum number of state machine steps before forcing termination
# Prevents infinite loops
agent.runtime.max-steps=4
# Maximum execution time in milliseconds before forcing termination
# Prevents runaway executions
agent.runtime.max-runtime-ms=15000
# ═══════════════════════════════════════════════════════════════
# SCHEMA VALIDATION
# ═══════════════════════════════════════════════════════════════
# Enable JSON schema validation for LLM responses
# When disabled, accepts any valid JSON
agent.runtime.schema-enforcement-enabled=true
# Enable auto-repair of invalid LLM responses
# LLM will be asked to fix its own output
agent.runtime.repair-enabled=true
# Number of repair attempts before falling back
agent.runtime.planner-validation-retries=1
# ═══════════════════════════════════════════════════════════════
# ORCHESTRATION MODE
# ═══════════════════════════════════════════════════════════════
# SINGLE_AGENT - Supervisor handles everything
# MULTI_AGENT - Supervisor delegates to specialists
agent.runtime.orchestration-mode=SINGLE_AGENT
```

---

## 3. Policy Thresholds

### Database Configuration

```sql
-- Policy thresholds are stored in the database for runtime modification

-- Minimum confidence for auto-routing
-- Below this = HUMAN_REVIEW
INSERT INTO policy_config (config_key, config_value, description)
VALUES ('AUTO_ROUTE_THRESHOLD', 0.75, 'Minimum confidence for autonomous routing');

-- Minimum confidence for CRITICAL priority tickets
-- CRITICAL + below this = HUMAN_REVIEW
INSERT INTO policy_config (config_key, config_value, description)
VALUES ('CRITICAL_MIN_CONF', 0.90, 'Minimum confidence for CRITICAL priority');

-- Maximum autonomous actions per ticket
INSERT INTO policy_config (config_key, config_value, description)
VALUES ('MAX_AUTONOMOUS_ACTIONS', 5, 'Maximum AI actions per ticket');

-- Maximum clarifying questions per ticket
INSERT INTO policy_config (config_key, config_value, description)
VALUES ('MAX_QUESTIONS_PER_TICKET', 3, 'Maximum questions AI can ask');
```

### Updating Thresholds at Runtime

```sql
-- Make system more conservative (require higher confidence)
UPDATE policy_config
SET config_value = 0.85
WHERE config_key = 'AUTO_ROUTE_THRESHOLD';

-- Allow more actions per ticket
UPDATE policy_config
SET config_value = 10
WHERE config_key = 'MAX_AUTONOMOUS_ACTIONS';

-- Clear cache to pick up changes (if using caching)
-- Application typically caches these values
```

---

## 4. Resilience Settings

### Complete Reference

```properties
# ═══════════════════════════════════════════════════════════════
# CIRCUIT BREAKER
# ═══════════════════════════════════════════════════════════════
# Number of calls to consider for failure rate calculation
resilience4j.circuitbreaker.instances.llmCircuit.sliding-window-size=10
# Failure rate percentage that opens the circuit
resilience4j.circuitbreaker.instances.llmCircuit.failure-rate-threshold=50
# Time to wait in OPEN state before trying again
resilience4j.circuitbreaker.instances.llmCircuit.wait-duration-in-open-state=30s
# Number of test calls allowed in HALF_OPEN state
resilience4j.circuitbreaker.instances.llmCircuit.permitted-number-of-calls-in-half-open-state=3
# Percentage of slow calls that opens the circuit
resilience4j.circuitbreaker.instances.llmCircuit.slow-call-rate-threshold=50
# What counts as a "slow" call
resilience4j.circuitbreaker.instances.llmCircuit.slow-call-duration-threshold=10s
# ═══════════════════════════════════════════════════════════════
# RETRY
# ═══════════════════════════════════════════════════════════════
# Total attempts (1 initial + retries)
resilience4j.retry.instances.llmRetry.max-attempts=3
# Initial wait before first retry
resilience4j.retry.instances.llmRetry.wait-duration=1s
# Enable exponential backoff
resilience4j.retry.instances.llmRetry.enable-exponential-backoff=true
# Multiplier for backoff (1s → 2s → 4s)
resilience4j.retry.instances.llmRetry.exponential-backoff-multiplier=2
# ═══════════════════════════════════════════════════════════════
# TIME LIMITER
# ═══════════════════════════════════════════════════════════════
# Maximum time for LLM call
resilience4j.timelimiter.instances.llmTimeout.timeout-duration=30s
# Cancel the future on timeout
resilience4j.timelimiter.instances.llmTimeout.cancel-running-future=true
```

---

## 5. Database Settings

### Connection Pool

```properties
# ═══════════════════════════════════════════════════════════════
# DATABASE CONNECTION POOL (HikariCP)
# ═══════════════════════════════════════════════════════════════
# Maximum pool size
spring.datasource.hikari.maximum-pool-size=20
# Minimum idle connections
spring.datasource.hikari.minimum-idle=5
# Connection timeout
spring.datasource.hikari.connection-timeout=30000
# Idle timeout before closing
spring.datasource.hikari.idle-timeout=600000
# Maximum connection lifetime
spring.datasource.hikari.max-lifetime=1800000
# ═══════════════════════════════════════════════════════════════
# JPA / HIBERNATE
# ═══════════════════════════════════════════════════════════════
# Show SQL (development only)
spring.jpa.show-sql=false
# Format SQL
spring.jpa.properties.hibernate.format_sql=true
# Statistics
spring.jpa.properties.hibernate.generate_statistics=false
```

---

## 6. Mode Reference

### Operating Modes

```
┌─────────────────────────────────────────────────────────────────┐
│                        OPERATING MODES                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ NORMAL MODE                                             │   │
│   │ enabled=true, shadow-mode=false                         │   │
│   │                                                         │   │
│   │ • Full state machine execution                          │   │
│   │ • Actions are executed                                  │   │
│   │ • Full tracing                                          │   │
│   │ • Use for: Production                                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ SHADOW MODE                                             │   │
│   │ enabled=true, shadow-mode=true                          │   │
│   │                                                         │   │
│   │ • Full state machine execution                          │   │
│   │ • Actions are NOT executed (dry run)                    │   │
│   │ • Full tracing for comparison                           │   │
│   │ • Use for: Testing new models/prompts                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ CLASSIC MODE                                            │   │
│   │ enabled=false                                           │   │
│   │                                                         │   │
│   │ • Single LLM call                                       │   │
│   │ • Policy engine still applies                           │   │
│   │ • Actions are executed                                  │   │
│   │ • Use for: Fallback, simpler routing                    │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ CANARY MODE                                             │   │
│   │ canary-enabled=true, allowed-queues=BILLING_Q,TECH_Q    │   │
│   │                                                         │   │
│   │ • Agent runtime only for specified queues               │   │
│   │ • Other queues use classic routing                      │   │
│   │ • Use for: Gradual rollout                              │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Anti-Patterns

### Configuration Mistakes to Avoid

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONFIGURATION ANTI-PATTERNS                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ❌ ANTI-PATTERN: Disabling schema validation in production     │
│   ──────────────────────────────────────────────────────────    │
│   schema-enforcement-enabled=false                              │
│                                                                 │
│   Risk: LLM can return invalid data, causing runtime errors     │
│   Better: Keep enabled, use repair-enabled=true instead         │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   ❌ ANTI-PATTERN: Very high max-steps                           │
│   ──────────────────────────────────────────────────────────    │
│   max-steps=50                                                  │
│                                                                 │
│   Risk: Runaway state machine, high latency, costs              │
│   Better: Keep low (3-5), rely on REFLECT to terminate          │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   ❌ ANTI-PATTERN: Very low AUTO_ROUTE_THRESHOLD                 │
│   ──────────────────────────────────────────────────────────    │
│   AUTO_ROUTE_THRESHOLD=0.30                                     │
│                                                                 │
│   Risk: Low-confidence decisions auto-execute                   │
│   Better: Keep at 0.75+ for safety                              │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   ❌ ANTI-PATTERN: Disabling circuit breaker                     │
│   ──────────────────────────────────────────────────────────    │
│   failure-rate-threshold=100                                    │
│                                                                 │
│   Risk: System tries indefinitely on LLM failure                │
│   Better: Keep at 50%, let circuit open                         │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   ❌ ANTI-PATTERN: No retry backoff                              │
│   ──────────────────────────────────────────────────────────    │
│   enable-exponential-backoff=false, wait-duration=0.1s          │
│                                                                 │
│   Risk: Thundering herd on LLM recovery                         │
│   Better: Use exponential backoff, 1s+ initial wait             │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   ❌ ANTI-PATTERN: Production without shadow mode testing        │
│   ──────────────────────────────────────────────────────────    │
│   Deploying new model directly to production                    │
│                                                                 │
│   Risk: Unknown behavior in production                          │
│   Better: Run in shadow mode first, compare results             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [08 - Observability](./08-observability.md)  
**Next:** [10 - Troubleshooting](./10-troubleshooting.md)
