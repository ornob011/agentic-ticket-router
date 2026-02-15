# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

### Prerequisites
- PostgreSQL 16 with pgvector: `docker compose up -d`
- Ollama running locally: `ollama serve`
- Required Ollama models: `qwen2.5:3b-instruct`, `nomic-embed-text`

### Backend (Maven)
```bash
./mvnw clean install                        # Full build (includes frontend)
./mvnw spring-boot:run                      # Run application on localhost:8080
./mvnw test                                 # Run all tests
./mvnw test -Dtest=ClassName                # Run specific test class
./mvnw test -Dtest=ClassName#methodName     # Run specific test method
```

### Frontend (Vite/React)
```bash
cd frontend && npm run dev                  # Development server with hot reload
cd frontend && npm run build                # Production build
cd frontend && npm run build:css            # Build Tailwind CSS
cd frontend && npm run watch:css            # Watch CSS for changes
cd frontend && npm run typecheck            # TypeScript check
```

## Code Style Guidelines

### Formatting
- 4-space indentation, 120 character line limit
- UTF-8 encoding, LF line endings
- No comments unless explicitly asked

### Naming Conventions
| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `SupportTicket`, `RouterService` |
| Methods/Variables | camelCase | `assignQueue`, `ticketId` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Database columns | snake_case | `last_activity_at` |
| Packages | lowercase | `com.dsi.support.agenticrouter` |

### Annotations
- Lombok: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j` for classes; `@Builder` for DTOs
- Services: `@Service`, `@Transactional`
- Controllers: `@Controller` (MVC), `@RestController` (API), `@RequestMapping`
- Entities: Extend `BaseEntity`, use `@Entity`, JPA annotations

### Entity Pattern
All entities extend `BaseEntity` which provides:
- `id`: Primary key (`@GeneratedValue(strategy=IDENTITY)`)
- `rowVersion`: Optimistic locking via `@Version`
- `createdAt`, `updatedAt`: Timestamps via `@CreationTimestamp`/`@UpdateTimestamp`
- `createdBy`, `updatedBy`: Audit fields via `@CreatedBy`/`@LastModifiedBy`

```java
@Entity
@Table(name = "support_ticket")
@Getter @Setter
@RequiredArgsConstructor
public class SupportTicket extends BaseEntity {
    @Column(nullable = false)
    private String subject;
    // ...
}
```

### DTO Pattern
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateTicketDto {
    private String subject;
    private String description;
    // ...
}
```

### Service Layer Pattern
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private final TicketRepository ticketRepository;

    @Transactional
    public Ticket createTicket(CreateTicketDto dto) {
        Long userId = Utils.getLoggedInUserId();
        // ...
    }
}
```

### Repository Pattern
- Extend `JpaRepository`
- Use named query methods: `findByStatusOrderByCreatedAtDesc(TicketStatus status)`
- Use `@Query` with JPQL for complex queries
- Use `@Param` for named parameters
- Use projections for custom result types (see `TicketQueueCount`, `AgentTicketCounts`)

```java
public interface TicketRepository extends JpaRepository<SupportTicket, Long> {
    @Query("SELECT t.status as status, COUNT(t) as count FROM SupportTicket t GROUP BY t.status")
    List<TicketStatusCount> countByStatus();
}
```

### Validation
- Custom validators implement `Validator` interface (see `CreateTicketValidator`)
- Use `ValidationUtils.rejectIfEmptyOrWhitespace` for required fields
- Use `@Valid` annotation on DTOs in controllers
- `BindingResult` follows the `@Valid` parameter for error handling

### Exception Handling
- Custom exceptions: `DataNotFoundException`, `ApplicationException`
- Use `DataNotFoundException.supplier(Class, id)` for `optional.orElseThrow` patterns
- Global handling via `@RestControllerAdvice` (`RestExceptionHandler`)
- Resilience4j for timeouts, retries, circuit breaker around Ollama calls

```java
SupportTicket ticket = ticketRepository.findById(id)
    .orElseThrow(DataNotFoundException.supplier(SupportTicket.class, id));
```

### Logging
- Use `@Slf4j` annotation
- Log levels: DEBUG, INFO, WARN, ERROR
- Log correlation ID per request
- Track `llm_output.latency_ms`, parse failures, repair attempts

## Architecture Overview

This is an AI-powered support ticket routing system using agentic AI patterns.

### Tech Stack

**Backend:** Spring Boot 3.5.9, Java 21, Spring Data JPA, PostgreSQL (pgvector), Spring AI 1.1.2

**Frontend:** React 18, TypeScript 5, Vite 6, Tailwind CSS 4, TanStack Query, React Router 7

**Infrastructure:** Docker Compose, Ollama (local LLM)

### Agentic AI Flow

1. Tickets enter at `RECEIVED` status via API
2. `AgenticStateMachine` orchestrates state transitions
3. LLM (Ollama/OpenAI) analyzes tickets via `RouterService`
4. `ActionRegistry` dispatches to appropriate `TicketAction` implementation
5. `PolicyEngine` applies confidence thresholds for human-in-the-loop decisions
6. All state changes emit audit events via `AuditService`

### State Machine

**State Flow:** `RECEIVED → TRIAGING → WAITING_CUSTOMER/ASSIGNED → IN_PROGRESS → RESOLVED/CLOSED/ESCALATED`

### Enum Types

| Enum | Values |
|------|--------|
| `ticket_status` | `RECEIVED`, `TRIAGING`, `WAITING_CUSTOMER`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `ESCALATED`, `AUTO_CLOSED_PENDING`, `CLOSED` |
| `ticket_category` | `BILLING`, `TECHNICAL`, `ACCOUNT`, `SHIPPING`, `SECURITY`, `OTHER` |
| `ticket_queue` | `BILLING_Q`, `TECH_Q`, `OPS_Q`, `SECURITY_Q`, `ACCOUNT_Q`, `GENERAL_Q` |
| `ticket_priority` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `next_action` | `AUTO_REPLY`, `ASK_CLARIFYING`, `ASSIGN_QUEUE`, `ESCALATE`, `HUMAN_REVIEW`, `AUTO_RESOLVE` |

### Human-in-the-Loop
- `confidence < threshold` → `HUMAN_REVIEW`
- `SECURITY`/`PII` detected → force `ESCALATE`

### Key Packages

| Package | Purpose |
|---------|---------|
| `controller/api/` | REST API endpoints |
| `service/routing/` | Core routing logic, state machine, LLM client |
| `service/action/` | Action pattern implementations for each `NextAction` type |
| `service/ai/` | LLM integration (Ollama), `PromptService` |
| `service/knowledge/` | Vector store, RAG for knowledge base |
| `service/dashboard/` | Role-based dashboard composition |
| `entity/` | JPA entities |
| `dto/` | Data Transfer Objects |
| `repository/` | Spring Data JPA repositories |
| `exception/` | Custom exceptions |

### Database Patterns
- ACID transactions for ticket creation + routing
- Index on: `status`, `assigned_queue`, `current_priority`, `last_activity_at`
- Ticket numbers: auto-increment `ticket_no` (BIGSERIAL), display as `TKT-00001234`
- Use JSONB for `rationale_tags` in `ticket_routing`
- Store raw LLM I/O in `llm_output` table, validated decisions in `ticket_routing`

### Frontend Architecture

```
frontend/src/
├── app/           # App context, providers
├── components/    # Reusable UI components (Radix UI primitives)
├── pages/         # Page components
├── lib/           # API clients, loaders, utilities
├── router.tsx     # React Router configuration
└── main.tsx       # Entry point
```

- The React SPA is built during Maven's `generate-resources` phase
- Build outputs to `src/main/resources/static/app/`
- API routes are prefixed with `/api/`
- Styling: Tailwind CSS 4.x (input: `frontend/src/input.css`, output: `src/main/resources/static/css/tailwind.css`)

## Security

- Role-based access: `ADMIN`, `SUPERVISOR`, `AGENT`, `CUSTOMER`
- Customer ownership enforced on ticket access
- Use `SecurityContextHolder.getContext().getAuthentication()` for auth
- Use `Utils.getLoggedInUserId()` to get authenticated user ID

## Configuration

- Main config: `src/main/resources/application.properties`
- Database: `dsi_agentic_ticketing` on localhost:5432 (user: `agentic_ticketing_app`)
- LLM: Ollama at localhost:11434 (fallback to OpenAI)
- Resilience4j: Circuit breaker 50% threshold, 30s wait; Retry 3 attempts with exponential backoff; 180s timeout for LLM calls

## Project Structure

```
/Volumes/Code/agentic-ticket-router/
├── pom.xml                           # Maven configuration
├── docker-compose.yml                # PostgreSQL + pgvector
├── src/main/java/com/dsi/support/agenticrouter/
│   ├── configuration/                # Spring configs (Security, Web, Async)
│   ├── controller/api/               # REST API controllers
│   ├── dto/                          # DTOs
│   ├── entity/                       # JPA entities (extend BaseEntity)
│   ├── enums/                        # Enumerations
│   ├── exception/                    # Custom exceptions
│   ├── repository/                   # Spring Data JPA
│   ├── security/                     # Security components
│   └── service/                      # Business logic
│       ├── action/                   # Action pattern
│       ├── ai/                       # LLM integration
│       ├── audit/                    # Audit trail
│       ├── auth/                     # Authentication
│       ├── dashboard/                # Dashboard composition
│       ├── knowledge/                # Vector store
│       ├── routing/                  # Core routing
│       └── ticket/                   # Ticket management
├── src/main/resources/
│   ├── application.properties
│   └── static/app/                   # Built React SPA
├── frontend/                         # React SPA
└── sql/
    ├── ddl/ddl.sql                   # Database schema
    └── dml/                          # Seed data
```
