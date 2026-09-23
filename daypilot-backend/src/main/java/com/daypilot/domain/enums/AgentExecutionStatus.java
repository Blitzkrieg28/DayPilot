package com.daypilot.domain.enums;

/**
 * Outcome states of a single {@link com.daypilot.domain.entity.AgentExecution} attempt.
 *
 * <p>This is deliberately separate from {@link TaskStatus}. A task may
 * have multiple execution attempts (retries), each with its own outcome.
 * The task-level status is derived from the aggregate of its executions
 * by the orchestrator; that logic does not belong in the domain model.</p>
 */
public enum AgentExecutionStatus {

    /** The agent is currently processing this execution attempt. */
    RUNNING,

    /** The agent completed this attempt successfully. */
    COMPLETED,

    /** The agent failed during this attempt. */
    FAILED
}
