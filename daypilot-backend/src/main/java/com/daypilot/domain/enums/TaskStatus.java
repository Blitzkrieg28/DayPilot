package com.daypilot.domain.enums;

/**
 * Lifecycle states of a {@link com.daypilot.domain.entity.WorkflowTask}.
 *
 * <p>Task status is managed by the workflow orchestrator and reflects
 * where the task sits in the DAG execution lifecycle.</p>
 */
public enum TaskStatus {

    /** Created but upstream dependencies have not been satisfied yet. */
    PENDING,

    /** All dependencies satisfied; eligible for agent dispatch. */
    READY,

    /** Currently being processed by an assigned agent. */
    RUNNING,

    /** Agent produced a side-effect that requires human approval. */
    AWAITING_APPROVAL,

    /** Finished successfully. */
    COMPLETED,

    /** Execution failed. */
    FAILED,

    /** Intentionally skipped (e.g. a prerequisite failed and this task is optional). */
    SKIPPED,

    /** User or system cancelled the task. */
    CANCELLED
}
