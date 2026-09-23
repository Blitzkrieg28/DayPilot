package com.daypilot.domain.enums;

/**
 * The six specialized agent types in the DayPilot system.
 *
 * <p>Each agent is a focused reasoning component backed by an LLM,
 * not an autonomous chatbot. The agent type determines which tools
 * and structured-output schemas are available to an execution.</p>
 */
public enum AgentType {

    /** Converts unstructured user requests into structured workflow DAGs. */
    PLANNING,

    /** Manages Google Calendar: availability, conflicts, event CRUD. */
    CALENDAR,

    /** Creates, decomposes, and prioritises tasks; manages deadlines. */
    TASK,

    /** Summarises email, extracts action items, drafts and sends messages. */
    EMAIL,

    /** RAG-based retrieval over Obsidian notes, documents, and personal context. */
    KNOWLEDGE,

    /** Cross-agent consistency checks, safety constraints, and schema validation. */
    VALIDATION
}
