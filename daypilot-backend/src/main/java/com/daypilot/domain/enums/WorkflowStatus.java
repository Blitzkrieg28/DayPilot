package com.daypilot.domain.enums;

/**
 * Lifecycle states of a {@link com.daypilot.domain.entity.Workflow}.
 *
 * <p>A workflow transitions through these states as the orchestrator
 * creates, executes, pauses, completes, or cancels it.</p>
 */
public enum WorkflowStatus {

    /** DAG created by the Planning Agent; no tasks executing yet. */
    PLANNED,

    /** At least one task is actively executing. */
    RUNNING,

    /** Workflow suspended (e.g. waiting for human approval). */
    PAUSED,

    /** All tasks finished successfully. */
    COMPLETED,

    /** Unrecoverable failure in one or more tasks. */
    FAILED,

    /** User or system cancelled the workflow. */
    CANCELLED
}
