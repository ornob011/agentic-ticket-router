# 12 - Production Checklist

> Complete checklist for deploying to production.

---

## Table of Contents

1. [Pre-Deployment Checklist](#1-pre-deployment-checklist)
2. [Security Checklist](#2-security-checklist)
3. [Configuration Checklist](#3-configuration-checklist)
4. [Monitoring Setup](#4-monitoring-setup)
5. [Go-Live Procedure](#5-go-live-procedure)
6. [Post-Deployment Verification](#6-post-deployment-verification)

---

## 1. Pre-Deployment Checklist

### Code Quality

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRE-DEPLOYMENT CHECKLIST                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ All code merged to main branch                              │
│   □ Code review completed                                       │
│   □ No hardcoded secrets in code                                │
│   □ No TODO/FIXME comments remaining                            │
│   □ Documentation updated                                       │
│   □ Database migrations tested                                  │
│   □ Backward compatible schema changes                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Testing

```
┌─────────────────────────────────────────────────────────────────┐
│                    TESTING CHECKLIST                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ Unit tests passing                                          │
│   □ Integration tests passing                                   │
│   □ Load testing completed                                      │
│   □ Shadow mode testing (minimum 24 hours)                      │
│   □ Failover testing completed                                  │
│   □ Security scan completed                                     │
│                                                                 │
│   Shadow Mode Validation:                                       │
│   ────────────────────────                                      │
│   □ Compare shadow vs classic decisions                         │
│   □ Review divergence cases                                     │
│   □ Validate latency within acceptable range                    │
│   □ Check for unexpected errors                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Security Checklist

### Secrets Management

```
┌─────────────────────────────────────────────────────────────────┐
│                    SECRETS CHECKLIST                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ Database credentials stored securely                        │
│     - AWS Secrets Manager / Vault / Environment variables       │
│                                                                 │
│   □ LLM API keys stored securely                                │
│     - Not in application.properties                             │
│     - Not in git repository                                     │
│                                                                 │
│   □ Session secrets configured                                  │
│     - Strong, unique value                                      │
│     - Rotated periodically                                      │
│                                                                 │
│   □ Encryption keys secured                                     │
│     - Password hashing secret                                   │
│     - Remember-me key                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Security Headers

```
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY HEADERS CHECKLIST                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ X-Content-Type-Options: nosniff                             │
│   □ X-Frame-Options: DENY                                       │
│   □ X-XSS-Protection: 1; mode=block                             │
│   □ Referrer-Policy: strict-origin-when-cross-origin            │
│   □ Content-Security-Policy configured                          │
│   □ Strict-Transport-Security (if HTTPS)                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Access Control

```
┌─────────────────────────────────────────────────────────────────┐
│                    ACCESS CONTROL CHECKLIST                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ Role-based access control tested                            │
│   □ All API endpoints have proper authorization                 │
│   □ Customer data isolated by tenant/user                       │
│   □ Admin endpoints restricted to ADMIN role                    │
│   □ Supervisor endpoints restricted properly                    │
│   □ No public access to sensitive operations                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Configuration Checklist

### Production Settings

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRODUCTION CONFIG CHECKLIST                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Agent Runtime:                                                │
│   ──────────────                                                │
│   □ agent.runtime.enabled=true                                  │
│   □ agent.runtime.shadow-mode=false                             │
│   □ agent.runtime.canary-enabled=false (or set queues)          │
│   □ agent.runtime.schema-enforcement-enabled=true               │
│   □ agent.runtime.repair-enabled=true                           │
│   □ agent.runtime.max-steps=4                                   │
│   □ agent.runtime.max-runtime-ms=15000                          │
│                                                                 │
│   Policy Thresholds:                                            │
│   ──────────────────                                            │
│   □ AUTO_ROUTE_THRESHOLD >= 0.75                                │
│   □ CRITICAL_MIN_CONF >= 0.90                                   │
│   □ MAX_AUTONOMOUS_ACTIONS <= 5                                 │
│   □ MAX_QUESTIONS_PER_TICKET <= 3                               │
│                                                                 │
│   Resilience:                                                   │
│   ───────────                                                   │
│   □ Circuit breaker configured                                  │
│   □ Retry with exponential backoff enabled                      │
│   □ Timeout configured (30s max)                                │
│   □ Fallback to HUMAN_REVIEW implemented                        │
│                                                                 │
│   Database:                                                     │
│   ───────────                                                   │
│   □ Connection pool sized appropriately                         │
│   □ SSL enabled for database connection                         │
│   □ Indexes created                                             │
│   □ Vector index created                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Monitoring Setup

### Logging

```
┌─────────────────────────────────────────────────────────────────┐
│                    LOGGING CHECKLIST                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ Structured logging enabled (JSON format)                    │
│   □ Log aggregation configured (ELK, CloudWatch, etc.)          │
│   □ Log retention policy set (30+ days)                         │
│   □ Sensitive data not logged                                   │
│   □ Correlation IDs included                                    │
│   □ Error logs trigger alerts                                   │
│                                                                 │
│   Key Log Patterns to Alert On:                                 │
│   ──────────────────────────────                                │
│   • "RoutingFallback" - LLM fallback used                       │
│   • "CircuitBreaker" open - LLM circuit opened                  │
│   • "Exception" - Unhandled exceptions                          │
│   • "PlanValidationFailed" - Schema validation failed           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Metrics

```
┌─────────────────────────────────────────────────────────────────┐
│                    METRICS CHECKLIST                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   □ Spring Actuator enabled                                     │
│   □ Prometheus endpoint exposed                                 │
│   □ Grafana dashboards created                                  │
│                                                                 │
│   Key Dashboards:                                               │
│   ────────────────                                              │
│   □ Routing latency over time                                   │
│   □ LLM success/failure rate                                    │
│   □ Circuit breaker state                                       │
│   □ Human review rate                                           │
│   □ Queue depths                                                │
│   □ Thread pool utilization                                     │
│   □ Database connection pool                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Alerts

```
┌─────────────────────────────────────────────────────────────────┐
│                    ALERT THRESHOLDS                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Critical (Page immediately):                                  │
│   ───────────────────────────                                   │
│   □ LLM success rate < 80% for 5 minutes                        │
│   □ Circuit breaker open for > 2 minutes                        │
│   □ Database connection pool exhausted                          │
│   □ Error rate > 10% for 5 minutes                              │
│                                                                 │
│   Warning (Investigate within 1 hour):                          │
│   ────────────────────────────────────                          │
│   □ LLM latency > 10 seconds (p95)                              │
│   □ Human review rate > 30%                                     │
│   □ Fallback rate > 10%                                         │
│   □ Routing queue depth > 50                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Go-Live Procedure

### Deployment Steps

```
┌─────────────────────────────────────────────────────────────────┐
│                    GO-LIVE PROCEDURE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Step 1: Pre-Deployment (30 min before)                        │
│   ──────────────────────────────────────                        │
│   □ Verify all tests passing                                    │
│   □ Verify shadow mode results acceptable                       │
│   □ Notify team of deployment                                   │
│   □ Prepare rollback plan                                       │
│                                                                 │
│   Step 2: Deployment                                            │
│   ────────────────                                              │
│   □ Deploy database migrations first                            │
│   □ Verify migrations successful                                │
│   □ Deploy application                                          │
│   □ Wait for health check to pass                               │
│                                                                 │
│   Step 3: Smoke Testing (immediately after)                     │
│   ──────────────────────────────────────                        │
│   □ Create test ticket                                          │
│   □ Verify routing completes successfully                       │
│   □ Check agent_runtime_run for success                         │
│   □ Verify no errors in logs                                    │
│   □ Test login/authentication                                   │
│                                                                 │
│   Step 4: Monitoring (first 30 minutes)                         │
│   ──────────────────────────────────                            │
│   □ Watch Grafana dashboards                                    │
│   □ Monitor error rates                                         │
│   □ Check LLM latency                                           │
│   □ Verify no circuit breaker opens                             │
│                                                                 │
│   Step 5: Canary Rollout (optional)                             │
│   ──────────────────────────────────                            │
│   If not ready for full rollout:                                │
│   □ Enable canary-enabled=true                                  │
│   □ Set allowed-queues=BILLING_Q                                │
│   □ Monitor for 1-2 hours                                       │
│   □ Add more queues gradually                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rollback Plan

```
┌─────────────────────────────────────────────────────────────────┐
│                    ROLLBACK PLAN                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Trigger Conditions:                                           │
│   ──────────────────                                            │
│   • Error rate > 20%                                            │
│   • Circuit breaker stays open > 5 minutes                      │
│   • Database issues                                             │
│   • Critical security issue                                     │
│                                                                 │
│   Rollback Steps:                                               │
│   ──────────────                                                │
│   1. Disable agent runtime immediately:                         │
│      kubectl set env deployment/app \                           │
│        AGENT_RUNTIME_ENABLED=false                              │
│                                                                 │
│   2. If needed, deploy previous version:                        │
│      kubectl rollout undo deployment/app                        │
│                                                                 │
│   3. Verify system stable with classic routing                  │
│                                                                 │
│   4. Investigate issue before re-deploying                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Post-Deployment Verification

### First 24 Hours

```
┌─────────────────────────────────────────────────────────────────┐
│                    POST-DEPLOYMENT VERIFICATION                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Hour 1:                                                       │
│   ───────────                                                   │
│   □ Monitor all dashboards continuously                         │
│   □ Review first 50 routing decisions                           │
│   □ Check for any error patterns                                │
│   □ Verify human review queue manageable                        │
│                                                                 │
│   Hours 2-4:                                                    │
│   ───────────                                                   │
│   □ Review routing success rate                                 │
│   □ Check fallback usage                                        │
│   □ Review LLM latency trends                                   │
│   □ Spot check routing decisions                                │
│                                                                 │
│   Hours 4-24:                                                   │
│   ─────────────                                                 │
│   □ Generate routing quality report                             │
│   □ Review policy trigger frequency                             │
│   □ Check customer feedback                                     │
│   □ Review agent workload impact                                │
│                                                                 │
│   After 24 Hours:                                               │
│   ────────────────                                              │
│   □ Full metrics review                                         │
│   □ Compare to pre-deployment baseline                          │
│   □ Document any issues encountered                             │
│   □ Plan optimizations if needed                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Success Criteria

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUCCESS CRITERIA                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Technical Metrics:                                            │
│   ──────────────────                                            │
│   □ LLM success rate > 95%                                      │
│   □ Average routing latency < 5 seconds                         │
│   □ Circuit breaker open rate < 1%                              │
│   □ Fallback rate < 5%                                          │
│   □ Zero critical errors                                        │
│                                                                 │
│   Business Metrics:                                             │
│   ──────────────────                                            │
│   □ Auto-routing rate > 70% (or target)                         │
│   □ Human review rate < 25%                                     │
│   □ No customer complaints about routing                        │
│   □ Agent workload reduced                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [11 - Performance Tuning](./11-performance-tuning.md)  
**Back to Index:** [README](./README.md)
