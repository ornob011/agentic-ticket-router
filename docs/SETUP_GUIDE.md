# Agentic Ticket Router

## Goal

AI-powered support ticket routing system for DSi AI Club. Automates ticket classification, prioritization, and queue
assignment using LLMs.

## Tech Stack

| Layer    | Technologies                                                    |
|----------|-----------------------------------------------------------------|
| Backend  | Spring Boot 3.5.9, Java 21, Spring Data JPA, Spring Security    |
| Database | PostgreSQL 16 + pgvector                                        |
| AI/ML    | Spring AI 1.1.2, Ollama (qwen2.5:3b-instruct, nomic-embed-text) |
| Frontend | React 18, Vite 6, Tailwind CSS 4                                |
| Build    | Maven, npm (via frontend-maven-plugin)                          |

## Prerequisites

- Java 21 JDK
- Node.js v22+
- Docker & Docker Compose
- IntelliJ IDEA (for SQL execution)
- Ollama

## Quick Start

### 1. Start Database

```bash
docker-compose up -d
```

### 2. Configure IntelliJ Database Connection

- Database: `dsi_agentic_ticketing`
- User: `agentic_ticketing_app`
- Password: `dev_strong_password_change_later`

### 3. Execute DDL (Schema)

Open `sql/ddl/ddl.sql` in IntelliJ → Run against database connection

### 4. Execute DML (Seed Data) - Run in Order

- `V001__seed_country.sql`
- `V002__seed_language.sql`
- `V003__seed_customer_tier.sql`
- `V004__seed_policy_config.sql`
- `V005__seed_model_registry.sql`
- `V006__seed_staff_users.sql`
- `V007__switch_to_qwen2.5:3b-instruct.sql`
- `V008__switch_to_qwen3:4b-instruct.sql`
- `V009__seed_global_config.sql`
- `V010__seed_knowledge_article.sql`
- `V011__seed_article_template.sql`
- `V012__seed_knowledge_article-comprehensive.sql`

### 5. Setup Ollama (Required)

```bash
ollama pull qwen2.5:3b-instruct
ollama pull nomic-embed-text
ollama serve
```

### 6. Vector Store Embeddings (Choose One)

**Option A - Pre-computed (Faster):**

- Download [V013__seed_vector_store.sql](https://drive.google.com/file/d/1_E5a4Sc-mbDUgGMdeAC-F7bXnBlIIiu4/view?usp=drive_link)
from Google Drive (~300 MB)
- Execute `V013__seed_vector_store.sql` in IntelliJ

**Option B - Generate at Startup:**

- Skip downloading
- Run app - embeddings generated automatically

### 7. Mark Initialized

Execute `V013__set_vector_store_initialized.sql` in IntelliJ if you chose **Option A** to seed the vector store.

### 8. Run Application

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

Access at: `http://localhost:8080`

## Frontend Development

> Output (`src/main/resources/static/app/`) is gitignored - build locally.

```bash
cd frontend
npm install
npm run dev        # Dev server with hot reload
npm run build      # Production build
npm run lint       # ESLint
npm run typecheck  # TypeScript check
```

## Default Users

All passwords: `AIClub123`

| Username         | Role       |
|------------------|------------|
| `admin`          | ADMIN      |
| `supervisor1`    | SUPERVISOR |
| `agent_billing`  | AGENT      |
| `agent_tech`     | AGENT      |
| `agent_security` | AGENT      |
| `agent_general`  | AGENT      |

## Configuration

- **Z.AI API Key**: Contact project maintainer for access
- Add to `.env` file in project root:
  ```
  ZAI_API_KEY=your_key_here
  ```

## Project Structure

```
src/main/java/.../agenticrouter/
├── configuration/   # Spring config, security
├── controller/      # REST & MVC controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── enums/           # Business enums
├── service/         # Business logic
├── repository/      # JPA repositories
└── util/            # Utilities

sql/
├── ddl/ddl.sql      # Schema
└── dml/             # Seed data (V001-V013)

frontend/            # React + Vite
```

## Useful Commands

```bash
./mvnw clean compile          # Compile
./mvnw spring-boot:run        # Run app
./mvnw clean package          # Build JAR
./mvnw test                   # Run tests
docker-compose up -d          # Start DB
docker-compose down           # Stop DB
```
