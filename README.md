# DayPilot

Stateful, tool-using multi-agent personal productivity assistant.

LLMs handle interpretation and reasoning. Deterministic backend services handle state, dependencies, permissions, validation, execution, and recovery.

## Architecture

See [DayPilot_Final_Architecture_Specification.md](DayPilot_Final_Architecture_Specification.md) for the full specification.

### Phase 1 — Foundation (current)

```
User → Planning Agent → Calendar Agent → Calendar Service → PostgreSQL → Response
```

**Stack:** Java 21, Spring Boot 4.1.1, Spring AI 2.0.1, PostgreSQL 17

### Future Phases

| Phase | Scope |
|-------|-------|
| 2 | Calendar Agent + Google Calendar API |
| 3 | Task, Email, Knowledge, Validation agents |
| 4 | RAG pipeline (Obsidian → pgvector) |
| 5 | Kafka async execution |
| 6 | Reliability (retries, idempotency, circuit breakers) |
| 7 | React frontend (chat, Kanban, workflow graph) |
| 8 | Observability (OpenTelemetry, Micrometer) |

## Prerequisites

- Java 21
- Docker & Docker Compose

## Quickstart

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env — set OPENAI_API_KEY and POSTGRES_PASSWORD

# 2. Start PostgreSQL
cd infrastructure
docker compose --env-file ../.env up -d

# 3. Build and run the backend
cd ../daypilot-backend
./mvnw spring-boot:run
```
