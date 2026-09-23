# DayPilot — Finalized Project Architecture & Technology Specification

**Status:** Finalized baseline for implementation  
**Project:** DayPilot — Stateful Multi-Agent Personal Productivity Assistant

## 1. Project Concept

DayPilot is a **stateful, tool-using multi-agent productivity system**.

The user provides an unstructured request. DayPilot converts it into a structured workflow, delegates meaningful units of work to specialized agents, validates the resulting plan, requests approval for consequential side effects, executes deterministic tools, persists the resulting state, and returns a concise summary.

### Core principle

**LLMs handle interpretation and reasoning. Deterministic backend services handle state, dependencies, permissions, validation, execution, and recovery.**

## 2. Final High-Level Flow

```text
User
  |
  v
Planning Agent
  |
  | structured workflow / DAG
  v
Workflow Orchestrator
  |
  +----------------+----------------+----------------+
  |                |                |                |
  v                v                v                v
Calendar Agent   Task Agent      Email Agent    Knowledge Agent
  |                |                |                |
  +----------------+----------------+----------------+
                           |
                           v
                  Validation Agent
                           |
                           v
                    Approval Gateway
                           |
                           v
                 Deterministic Tools
                           |
                           v
                      Final State
                           |
                           v
                    Final Summary
```

Kafka is introduced between the orchestrator and worker agents when asynchronous/distributed execution is enabled.

## 3. Final Six Agents

### 3.1 Planning Agent
Converts messy natural-language requests into structured workflows.

Determines:
- intents
- meaningful task boundaries
- dependencies
- priorities
- required agent
- required tools
- constraints
- expected outputs

It does **not** directly execute side effects.

### 3.2 Calendar Agent
Responsible for:
- checking availability
- detecting conflicts
- proposing events
- creating events
- updating events
- deleting/canceling events
- rescheduling

External integration: **Google Calendar API**.

### 3.3 Task & Priority Agent
Responsible for:
- creating tasks
- decomposing goals into meaningful tasks
- prioritization
- deadlines
- recurring tasks
- task dependencies
- task status

Task state lives in PostgreSQL.

### 3.4 Email Agent
Responsible for:
- email summarization
- extracting action items
- drafting replies
- drafting new emails
- preparing messages for sending

External integration: **Gmail API**.

Safety model:

```text
Generate -> Draft -> Show User -> Approve/Edit/Reject -> Send
```

### 3.5 Knowledge & Personal Context Agent
Combines personal knowledge retrieval and grocery/inventory functionality.

Personal knowledge:
- Obsidian/Markdown notes
- project information
- goals
- preferences
- meeting notes
- documents

Grocery/inventory:
- grocery list
- household inventory
- low-stock items
- shopping suggestions

RAG pipeline:

```text
Obsidian / Markdown / Documents
             |
             v
      Ingestion Pipeline
             |
             v
          Chunking
             |
             v
         Embeddings
             |
             v
          pgvector
             |
             v
       Retrieval Service
             |
             v
      Knowledge Agent
             |
             v
             LLM
```

### 3.6 Validation & Safety Agent
Checks:
- dependency consistency
- task validity
- calendar conflicts
- invalid time ranges
- permission/safety constraints
- structured schemas
- tool-call validity
- potentially dangerous operations

Critical constraints should not depend solely on an LLM; use deterministic rules where possible.

## 4. Components That Are NOT Agents

Keep these deterministic:
- Workflow Orchestrator
- Workflow State Manager
- DAG/dependency engine
- Approval Gateway
- Calendar Service
- Email Service
- Task Service
- Grocery/Inventory Service
- Notification Scheduler
- RAG ingestion pipeline
- Authentication/authorization
- Database layer
- Kafka consumers/producers
- Observability layer

**LLM = interpretation + reasoning + proposal**  
**Backend = validation + state + execution + recovery**

## 5. Final Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 4.1.x |
| AI integration | Spring AI 2.0.x |
| Primary LLM | OpenAI API initially |
| Embeddings | OpenAI embeddings initially |
| Database | PostgreSQL |
| Vector search | pgvector |
| Messaging | Apache Kafka 4.x |
| Cache | Redis |
| Observability | Micrometer + OpenTelemetry |
| Infrastructure | Docker + Docker Compose |
| Frontend | React + Vite |
| Styling | Tailwind CSS |
| Real-time updates | WebSocket/SSE |
| Calendar | Google Calendar API |
| Email | Gmail API |
| Personal knowledge | Obsidian/Markdown |
| Authentication | Spring Security + OAuth 2.0 |
| Agent communication | Typed JSON / Java DTOs |
| Agent tools | Spring AI tool calling |
| Future tool protocol | MCP |
| Workflow engine | Custom DayPilot orchestrator |
| Durable workflow engine | Not used initially |

## 6. Why Spring AI?

Spring AI is the primary AI integration layer for:
- LLM abstraction
- structured outputs
- tool calling
- embeddings
- RAG
- model integration

DayPilot should **not** use LangGraph as its primary runtime. Instead:

```text
Spring Boot
    |
    v
DayPilot Workflow Orchestrator
    |
    v
Spring AI Agents
```

LangGraph and other frameworks are studied for architectural ideas rather than stacked into the runtime.

## 7. Why Kafka?

Kafka demonstrates genuine asynchronous, distributed agent execution.

Example topics/events:

```text
workflow.created
workflow.tasks.ready

agent.calendar.requested
agent.task.requested
agent.email.requested
agent.knowledge.requested

agent.task.completed
agent.task.failed

workflow.validation.requested
workflow.approval.requested

workflow.completed
```

Kafka is the **event transport layer**, not the source of truth.

## 8. PostgreSQL — Source of Truth

PostgreSQL stores durable application state.

Conceptual entities:

```text
workflow
workflow_task
task_dependency
agent_execution
tool_execution
approval
calendar_event
personal_task
grocery_item
memory
```

PostgreSQL stores workflow/task/agent/approval/tool state.

Kafka transports events.

Redis provides temporary acceleration.

## 9. pgvector

Use PostgreSQL + pgvector instead of a separate vector database initially.

```text
PostgreSQL
  |
  +-- Application tables
  |
  +-- pgvector
          |
          +-- embeddings
```

## 10. Redis

Use Redis for:
- caching
- rate limiting
- temporary locks
- short-lived session information
- appropriate request deduplication

Do not make Redis the durable workflow source of truth.

## 11. External APIs

Freeze initial external integrations to:

### OpenAI API
Planning, reasoning, structured generation, summarization, embeddings.

### Google Calendar API
Events, availability, scheduling, conflict detection.

### Gmail API
Reading relevant mail, summarization, drafting, sending after approval.

Do not initially add Slack, Twilio, WhatsApp, Telegram, Notion, Microsoft Calendar, shopping APIs, etc.

## 12. Obsidian Integration

Obsidian is a personal knowledge source, not the workflow engine.

Initial integration:

```text
Obsidian Vault
      |
      v
Markdown files
      |
      v
DayPilot ingestion
      |
      v
Chunking + metadata
      |
      v
Embeddings
      |
      v
pgvector
```

Start with reading/importing Markdown rather than full bidirectional synchronization.

## 13. Agent-to-Agent Communication

Agents should **not** communicate through free-form conversational prose.

Use structured contracts.

Example:

```json
{
  "workflowId": "WF-102",
  "taskId": "T1",
  "agent": "CALENDAR",
  "action": "PROPOSE_EVENT",
  "payload": {
    "title": "Backend Coding",
    "durationMinutes": 120,
    "preferredWindow": "MORNING"
  }
}
```

Every agent follows:

```text
Typed Input
    |
    v
LLM Reasoning
    |
    v
Structured Output
    |
    v
Validation
    |
    v
Tool Execution
```

## 14. Workflow / DAG Model

Every user request becomes a workflow.

```text
Workflow
 |
 +-- Task T1
 |
 +-- Task T2
 |
 +-- Task T3
 |
 +-- Task T4
```

Dependencies are explicit.

Example:

```text
T1 ──────┐
         +──> T3
T2 ──────┘

T4
```

T1 and T2 can execute concurrently; T3 waits for both.

The orchestrator determines which tasks are ready.

## 15. Human-in-the-Loop

### READ
Normally no approval:
- Read calendar
- Read notes
- Read email

### PROPOSE
Normally safe to generate:
- Propose schedule
- Draft email
- Generate grocery list

### SIDE EFFECT
Approval required by default:
- Send email
- Create important calendar event
- Delete event
- Modify important task

## 16. Kanban / Munder-Difflin-Style UI

The Kanban board is the **workflow control and observability layer**, not the orchestrator.

Example:

```text
+-----------+------------+-------------+------------+
| PLANNED   | RUNNING    | REVIEW      | COMPLETED  |
+-----------+------------+-------------+------------+
| Research  | Calendar   | Send Email  | Grocery    |
|           | scheduling |             | list       |
|           |            | ⚠ APPROVAL  |            |
+-----------+------------+-------------+------------+
```

Live activity can show:

```text
16:42:01 Planning Agent → created 5 tasks
16:42:02 Calendar Agent → checking availability
16:42:02 Knowledge Agent → retrieved 4 notes
16:42:03 Task Agent → created 3 tasks
16:42:04 Validation → conflict detected
16:42:05 Workflow → waiting for approval
```

The UI reflects backend state. If the browser closes, the workflow should continue.

## 17. Observability

Use **Micrometer + OpenTelemetry**.

Trace:

```text
User Request
    |
Planner LLM
    |
Workflow Creation
    |
Kafka Publish
    |
Agent Execution
    |
Tool Call
    |
Database
    |
Final Response
```

Collect:
- end-to-end latency
- agent latency
- LLM latency
- token usage
- LLM call count
- Kafka latency
- task execution time
- retries
- failures
- queue depth
- workflow completion
- RAG retrieval latency

## 18. Docker Infrastructure

Use Docker Compose for local infrastructure.

Expected services:

```text
postgres
kafka
redis
otel-collector
prometheus
grafana
```

## 19. Temporal

Do **not** add Temporal initially.

Temporal is valuable to study, but combining Kafka + Temporal + Spring Boot + PostgreSQL would unnecessarily complicate the first implementation.

The goal is to learn the workflow orchestration and distributed execution layer ourselves.

## 20. MCP

MCP should be studied and potentially added after the core system works.

Initial tool architecture:

```text
Spring AI
   |
   v
@Tool
   |
   v
DayPilot Service
```

Later:

```text
DayPilot
   |
   v
MCP Client
   |
   +-- Calendar MCP
   +-- Email MCP
   +-- Files MCP
```

## 21. Existing Agent Systems to Study

| System | Main idea to study |
|---|---|
| LangGraph | Stateful graph workflows, checkpoints, interrupts |
| CrewAI | Specialized agent roles and task delegation |
| OpenAI Agents SDK | Tools, handoffs, guardrails, tracing |
| Hermes | Persistent memory, skills, scheduled workflows |
| Microsoft Agent Framework | Workflow composition and human-in-the-loop |
| LlamaIndex | RAG and knowledge workflows |
| MCP | Standardized tool interfaces |

Borrow concepts; do not combine every framework into DayPilot.

## 22. Removed / Simplified Components

### Reminder Agent
Handled through Task/Calendar services and a deterministic notification scheduler.

### Synthesizer Agent
Do not maintain a permanently separate Synthesizer Agent.

Instead:

```text
Specialized agents
      |
      v
Validation
      |
      v
Deterministic result aggregation
      |
      v
Final response generation
```

A lightweight LLM call may generate the final natural-language summary.

## 23. Final Architecture

```text
                         +------------------+
                         |    React UI      |
                         | Chat + Kanban    |
                         +--------+---------+
                                  |
                           REST / WebSocket
                                  |
                                  v
                    +-------------------------+
                    | Spring Boot Application |
                    |                         |
                    | Workflow Orchestrator   |
                    | State Manager            |
                    | Approval Manager         |
                    +------------+------------+
                                 |
                         Planning Agent
                                 |
                          Structured DAG
                                 |
                 +---------------+----------------+
                 |               |                |
                 v               v                v
              Kafka Topics / Async Task Events
                 |               |                |
          +------+-----+   +-----+------+   +-----+------+
          |            |   |            |   |            |
          v            v   v            v   v            v
     Calendar      Task Agent       Email Agent   Knowledge Agent
       Agent
          |            |            |              |
          +------------+------------+--------------+
                                 |
                                 v
                       Validation Agent
                                 |
                                 v
                         Approval Gateway
                                 |
                                 v
                    Deterministic Tool Layer
                         /       |                               /        |                               v         v         v
                   Google      Gmail    PostgreSQL
                  Calendar
                                 |
                                 v
                            Final State
                                 |
                    +------------+------------+
                    |                         |
                    v                         v
               PostgreSQL                  pgvector
               Workflow State            RAG Knowledge
                                          Obsidian
                                          Documents
                    |
                    v
              Observability
          Micrometer + OpenTelemetry
```

## 24. Development Roadmap

### Phase 1 — Foundation

```text
Spring Boot
+
PostgreSQL
+
Spring AI
+
one LLM
```

Flow:

```text
User
 ↓
Planning Agent
 ↓
Task Graph
 ↓
PostgreSQL
 ↓
Response
```

### Phase 2 — First real agent

```text
Calendar Agent
 ↓
Google Calendar
```

### Phase 3 — Remaining agents

```text
Task Agent
Email Agent
Knowledge Agent
Validation Agent
```

### Phase 4 — RAG

```text
Obsidian
 ↓
Ingestion
 ↓
Embeddings
 ↓
pgvector
 ↓
Knowledge Agent
```

### Phase 5 — Kafka

Convert direct calls into asynchronous event-driven execution.

### Phase 6 — Reliability

Add:
- retries
- idempotency
- dead-letter topics
- timeouts
- circuit breakers
- failure recovery

### Phase 7 — UI

Build:
- chat interface
- Kanban board
- workflow graph
- agent activity feed
- approval cards
- task details

### Phase 8 — Observability

Add:
- OpenTelemetry
- Micrometer
- metrics
- traces
- token accounting
- latency dashboards

## 25. Final Design Principle

DayPilot is **not six autonomous chatbots talking to one another**.

It is:

```text
Six specialized reasoning components
                +
Deterministic workflow orchestration
                +
Event-driven distributed execution
                +
Persistent state
                +
RAG / personal context
                +
Controlled real-world tools
                +
Human approval
                +
Observability
```

The project demonstrates:
- LLMs
- agentic AI
- tool calling
- structured outputs
- RAG
- memory
- workflow/DAG orchestration
- Kafka
- asynchronous processing
- distributed systems
- PostgreSQL
- vector search
- reliability
- observability
- human-in-the-loop AI
- real external integrations
- full-stack engineering

## Implementation Rule

Do **not** begin by building Kafka or all six agents.

Start with:

```text
Planner
   ↓
Calendar Agent
   ↓
Deterministic Calendar Service
   ↓
PostgreSQL
   ↓
Final Response
```

Get this end-to-end flow working first.

Then progressively introduce the remaining agents, RAG, Kafka, reliability, UI, and observability.
