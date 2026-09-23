package com.daypilot.domain.entity;

import com.daypilot.domain.enums.AgentExecutionStatus;
import com.daypilot.domain.enums.AgentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable audit record of a single agent execution attempt.
 *
 * <p>A {@link WorkflowTask} may have multiple executions (retries). Each
 * execution records what the agent received, what it produced, and whether
 * it succeeded or failed. The task-level {@code TaskStatus} is determined
 * by the orchestrator based on the aggregate outcome of all executions;
 * that logic is <em>not</em> in this entity.</p>
 *
 * <h3>Relationship: AgentExecution to WorkflowTask (many-to-one)</h3>
 * <ul>
 *   <li><b>fetch = LAZY</b> &mdash; An execution record is often queried
 *       in isolation (e.g. for observability dashboards or retry decisions).
 *       Lazy loading avoids pulling in the full task and its transitive
 *       relationships.</li>
 *   <li><b>optional = false</b> &mdash; Every execution attempt must belong
 *       to a task.</li>
 *   <li><b>No cascade</b> &mdash; Execution records are audit logs. Deleting
 *       an execution should never affect the parent task, and the task
 *       does not cascade to executions either (preserving audit history).</li>
 * </ul>
 *
 * <p>Uses {@link AgentExecutionStatus} (RUNNING / COMPLETED / FAILED) rather
 * than {@link com.daypilot.domain.enums.TaskStatus} because the execution
 * outcome space is smaller and semantically distinct from the task lifecycle.</p>
 */
@Entity
@Table(name = "agent_executions")
public class AgentExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The task this execution attempt belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private WorkflowTask task;

    /** Which agent type performed this execution. */
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 20)
    private AgentType agentType;

    /** Outcome of this specific execution attempt. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentExecutionStatus status;

    /** JSON input that was given to the agent for this attempt. */
    @Column(name = "input_payload", columnDefinition = "text")
    private String inputPayload;

    /** JSON output returned by the agent after this attempt. */
    @Column(name = "output_payload", columnDefinition = "text")
    private String outputPayload;

    /** Error details if this attempt failed; null on success. */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** When this execution attempt started. */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** When this execution attempt finished (null while RUNNING). */
    @Column(name = "completed_at")
    private Instant completedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** No-arg constructor required by JPA. */
    protected AgentExecution() {
    }

    /**
     * Application constructor for starting a new execution attempt.
     *
     * @param task      the task being executed
     * @param agentType which agent is executing
     * @param status    initial status (typically {@code RUNNING})
     * @param startedAt when execution began
     */
    public AgentExecution(WorkflowTask task, AgentType agentType,
                          AgentExecutionStatus status, Instant startedAt) {
        this.task = task;
        this.agentType = agentType;
        this.status = status;
        this.startedAt = startedAt;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public WorkflowTask getTask() {
        return task;
    }

    public void setTask(WorkflowTask task) {
        this.task = task;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public AgentExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(AgentExecutionStatus status) {
        this.status = status;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
