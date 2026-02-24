# 11 - Performance Tuning

> Optimizing LLM, database, caching, and concurrency for production.

---

## Table of Contents

1. [LLM Optimization](#1-llm-optimization)
2. [Database Optimization](#2-database-optimization)
3. [Caching Strategy](#3-caching-strategy)
4. [Concurrency Tuning](#4-concurrency-tuning)
5. [Vector Store Optimization](#5-vector-store-optimization)
6. [Performance Metrics](#6-performance-metrics)

---

## 1. LLM Optimization

### Model Selection

```
┌─────────────────────────────────────────────────────────────────┐
│                    MODEL SELECTION GUIDE                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Development / Testing:                                        │
│   ──────────────────────                                        │
│   • Ollama with qwen3:4b-instruct (fast, local)                 │
│   • Lower quality acceptable for testing                        │
│                                                                 │
│   Production:                                                   │
│   ───────────                                                   │
│   • Ollama with qwen3:14b-instruct or larger                    │
│   • OpenAI gpt-5-mini (cost-effective)                          │
│   • OpenAI gpt-5.2 (higher quality)                             │
│                                                                 │
│   Trade-offs:                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Model          │ Latency  │ Quality │ Cost    │ Size    │   │
│   ├─────────────────────────────────────────────────────────┤   │
│   │ llama3.2:1b    │ ~1s      │ Low     │ Free    │ 1.3GB   │   │
│   │ llama3.2:3b    │ ~2s      │ Medium  │ Free    │ 2GB     │   │
│   │ llama3.1:8b    │ ~4s      │ Good    │ Free    │ 4.7GB   │   │
│   │ gpt-4o-mini    │ ~1-2s    │ Good    │ $0.15/1M│ N/A     │   │
│   │ gpt-4o         │ ~3-5s    │ Best    │ $2.50/1M│ N/A     │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Temperature and Token Limits

```
┌─────────────────────────────────────────────────────────────────┐
│                        LLM PARAMETERS                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Temperature:                                                  │
│   ────────────                                                  │
│   • 0.0 - Most deterministic (recommended for routing)          │
│   • 0.3 - Low variability                                       │
│   • 0.7 - Default, some creativity                              │
│   • 1.0 - Maximum randomness (avoid for routing)                │
│                                                                 │
│   Recommendation: 0.1 - 0.3 for structured output               │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Token Limits:                                                 │
│   ──────────────                                                │
│   • Input: Keep under 4000 tokens for speed                     │
│   • Output: 500-1000 tokens sufficient for routing response     │
│   • Truncate conversation history if needed                     │
│                                                                 │
│   ───────────────────────────────────────────────────────────── │
│                                                                 │
│   Response Format:                                              │
│   ─────────────────                                             │
│   • Use JSON mode if available (guarantees valid JSON)          │
│   • Constrain output with schema                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Reducing LLM Latency

```
┌─────────────────────────────────────────────────────────────────┐
│                    LATENCY REDUCTION STRATEGIES                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. REDUCE CONTEXT SIZE                                        │
│      • Limit conversation history to 5 messages                 │
│      • Limit RAG articles to 3-5                                │
│      • Truncate long messages                                   │
│                                                                 │
│   2. USE SMALLER MODELS FOR SIMPLE TASKS                        │
│      • Use smaller model for initial classification             │
│      • Use larger model only for complex decisions              │
│                                                                 │
│   3. BATCH REQUESTS                                             │
│      • Process multiple tickets in single LLM call              │
│      • (Future optimization - not currently implemented)        │
│                                                                 │
│   4. STREAMING                                                  │
│      • Stream LLM response for faster time-to-first-token       │
│      • (Already implemented for draft generation)               │
│                                                                 │
│   5. CACHING                                                    │
│      • Cache similar ticket routing decisions                   │
│      • Pattern matching before LLM call                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Database Optimization

### Index Recommendations

```sql
-- Essential indexes for agent runtime

-- Agent runtime run queries
CREATE INDEX idx_agent_runtime_run_ticket ON agent_runtime_run (ticket_id);
CREATE INDEX idx_agent_runtime_run_status ON agent_runtime_run (status);
CREATE INDEX idx_agent_runtime_run_started ON agent_runtime_run (started_at);

-- Agent runtime step queries
CREATE INDEX idx_agent_runtime_step_run ON agent_runtime_step (run_id);
CREATE INDEX idx_agent_runtime_step_type ON agent_runtime_step (step_type);

-- Ticket queries
CREATE INDEX idx_support_ticket_status ON support_ticket (status);
CREATE INDEX idx_support_ticket_queue ON support_ticket (assigned_queue);
CREATE INDEX idx_support_ticket_customer ON support_ticket (customer_id);
CREATE INDEX idx_support_ticket_updated ON support_ticket (updated_at);

-- Routing queries
CREATE INDEX idx_ticket_routing_ticket ON ticket_routing (ticket_id);
CREATE INDEX idx_ticket_routing_version ON ticket_routing (ticket_id, version);

-- LLM output queries
CREATE INDEX idx_llm_output_ticket ON llm_output (ticket_id);
CREATE INDEX idx_llm_output_status ON llm_output (parse_status);
```

### Query Optimization

```
┌──────────────────────────────────────────────────────────────────┐
│                    QUERY OPTIMIZATION                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Use JPA Projections:                                           │
│   ────────────────────                                           │
│   Don't load full entities for list views:                       │
│                                                                  │
│   @Query("SELECT t.id, t.subject, t.status FROM SupportTicket t")│
│   List<TicketSummary> findSummaries();                           │
│                                                                  │
│   ─────────────────────────────────────────────────────────────  │
│                                                                  │
│   Keyset Pagination:                                             │
│   Better than Offset for large datasets (O(1) vs O(N)):          │
│                                                                  │
│   @Query("SELECT t FROM SupportTicket t " +                      │
│          "WHERE t.id > :lastId ORDER BY t.id ASC")               │
│   List<Ticket> findNextPage(@Param("lastId") Long lastId,        │
│                             Pageable pageable);                  │
│                                                                  │
│   ─────────────────────────────────────────────────────────────  │
│                                                                  │
│   Batch Fetching:                                                │
│   ───────────────                                                │
│   Use @BatchSize to solve N+1 lazily:                            │
│                                                                  │
│   @BatchSize(size = 20)                                          │
│   @OneToMany(mappedBy = "ticket")                                │
│   List<TicketMessage> messages;                                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Connection Pool Sizing

```properties
# Formula: connections = (core_count * 2) + disk_spindles
# For 4-core machine:
spring.datasource.hikari.maximum-pool-size=10
# For 8-core machine:
spring.datasource.hikari.maximum-pool-size=20
# For high-traffic production:
spring.datasource.hikari.maximum-pool-size=30
# Minimum idle (keep some ready)
spring.datasource.hikari.minimum-idle=5
# Connection timeout
spring.datasource.hikari.connection-timeout=30000
```

---

## 3. Caching Strategy

### What's Cached

```
┌─────────────────────────────────────────────────────────────────┐
│                    CACHING OVERVIEW                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Currently Cached:                                             │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   Policy Config Values                                  │   │
│   │   @Cacheable("policyConfig")                            │   │
│   │   • AUTO_ROUTE_THRESHOLD                                │   │
│   │   • CRITICAL_MIN_CONF                                   │   │
│   │   • Evicted on config change                            │   │
│   │                                                         │   │
│   │   Active Model                                          │   │
│   │   @Cacheable("activeModel")                             │   │
│   │   • Current active model tag (e.g., 'gemini-2.0-flash') │   │
│   │   • Evicted on model change                             │   │
│   │                                                         │   │
│   │   Pattern Hints                                         │   │
│   │   @Cacheable("patternHints")                            │   │
│   │   • Routing patterns by category                        │   │
│   │   • Time-based eviction (TTL: 10m)                      │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Not Cached (by design):                                       │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                                                         │   │
│   │   • Ticket data (frequently updated state)              │   │
│   │   • Routing decisions (highly context-dependent)        │   │
│   │   • LLM responses (expensive, unique token streams)     │   │
│   │                                                         │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Cache Configuration

```properties
# Enable caching
spring.cache.type=caffeine
# Cache specs (if using Caffeine)
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
```

---

## 4. Concurrency Tuning

### Thread Pool Sizing

```
┌─────────────────────────────────────────────────────────────────┐
│                    THREAD POOL SIZING                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Tomcat (HTTP requests):                                       │
│   ──────────────────────────                                    │
│   server.tomcat.threads.max=200                                 │
│   server.tomcat.threads.min-spare=10                            │
│                                                                 │
│   Routing Thread Pool:                                          │
│   ────────────────────────                                      │
│   Size based on LLM throughput:                                 │
│                                                                 │
│   • If LLM handles 5 concurrent requests: pool = 5              │
│   • If LLM handles 20 concurrent requests: pool = 15-20         │
│                                                                 │
│   spring.task.execution.pool.core-size=5                        │
│   spring.task.execution.pool.max-size=10                        │
│   spring.task.execution.pool.queue-capacity=100                 │
│   spring.task.execution.pool.keep-alive=60s                     │
│                                                                 │
│   Why not larger?                                               │
│   • LLM is the bottleneck, not threads                          │
│   • More threads = more LLM contention                          │
│   • Better to queue than overload LLM (avoid 429 Errors)        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Rate Limiting (Recommended)

```properties
# Add rate limiting for public endpoints
resilience4j.ratelimiter.instances.ticketCreate.limitForPeriod=10
resilience4j.ratelimiter.instances.ticketCreate.limitRefreshPeriod=1m
resilience4j.ratelimiter.instances.ticketCreate.timeoutDuration=0
resilience4j.ratelimiter.instances.llmInference.limitForPeriod=100
resilience4j.ratelimiter.instances.llmInference.limitRefreshPeriod=1m
resilience4j.ratelimiter.instances.llmInference.timeoutDuration=0

```

---

## 5. Vector Store Optimization

### Index Configuration

```sql
-- Create IVFFlat index for approximate search
CREATE INDEX ON knowledge_article
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- For larger datasets, use HNSW
CREATE INDEX ON knowledge_article
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

### Search Parameters

```
┌─────────────────────────────────────────────────────────────────┐
│                    SEARCH PARAMETERS                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Current Settings:                                             │
│   ──────────────────                                            │
│   TOP_K_ARTICLES = 5                                            │
│   ARTICLE_SIMILARITY_THRESHOLD = 0.82                           │
│                                                                 │
│   Tuning:                                                       │
│   ────────                                                      │
│   • Lower TOP_K = faster, less context, lower token cost        │
│   • Higher TOP_K = slower, more context, higher risk of "noise" │
│   • Higher threshold = fewer, high-precision results            │
│   • Lower threshold = more "maybe" results (higher recall)      │
│                                                                 │
│   Recommendations:                                              │
│   ────────────────                                              │
│   • Production: TOP_K=3, threshold=0.85 (Focus on Precision)    │
│   • Development: TOP_K=5, threshold=0.75 (Focus on Exploration) │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Performance Metrics

### Key Metrics to Monitor

```
┌─────────────────────────────────────────────────────────────────┐
│                          KEY METRICS                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   LLM Metrics:                                                  │
│   ────────────                                                  │
│   • routing.decision.latency   - Time for LLM decision          │
│   • routing.decision.count     - Total decisions                │
│   • routing.fallback.count     - Fallback usage                 │
│   • circuit_breaker.state      - Circuit status                 │
│                                                                 │
│   Database Metrics:                                             │
│   ──────────────────                                            │
│   • hikaricp.connections.active  - Active DB connections        │
│   • hikaricp.connections.pending - Waiting for connection       │
│   • query.latency                - Query execution time         │
│                                                                 │
│   Application Metrics:                                          │
│   ─────────────────────                                         │
│   • jvm.threads.live      - Total threads                       │
│   • jvm.memory.used       - Memory usage                        │
│   • http.server.requests  - HTTP latency                        │
│                                                                 │
│   Business Metrics:                                             │
│   ──────────────────                                            │
│   • routing.success_rate  - % of auto-routed tickets            │
│   • human_review.rate     - % requiring human review            │
│   • avg_routing_time      - End-to-end routing latency          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Target Performance

```
┌─────────────────────────────────────────────────────────────────┐
│                    TARGET PERFORMANCE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Metric                    │ Target         │ Warning          │
│   ══════════════════════════╪════════════════╪════════════════  │
│   LLM latency               │ < 3 seconds    │ > 10 seconds     │
│   Total routing time        │ < 5 seconds    │ > 15 seconds     │
│   Database query            │ < 50 ms        │ > 200 ms         │
│   Circuit breaker open rate │ < 1%           │ > 5%             │
│   Fallback rate             │ < 5%           │ > 15%            │
│   Human review rate         │ < 20%          │ > 40%            │
│   Queue depth               │ < 50           │ > 200            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Navigation

**Previous:** [10 - Troubleshooting](./10-troubleshooting.md)  
**Next:** [12 - Production Checklist](./12-production-checklist.md)
