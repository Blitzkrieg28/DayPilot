package com.daypilot.domain.entity;

import com.daypilot.domain.enums.AgentType;
import com.daypilot.domain.enums.TaskStatus;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single node in a workflow's directed acyclic graph (DAG).
 *
 * <p>Each task represents a meaningful unit of work assigned to exactly one
 * {@link AgentType}. Tasks do <em>not</em> carry an {@code executionOrder} field;
 * the DAG engine will dynamically determine which tasks are ready for execution
 * by inspecting {@link TaskDependency} relationships. Independent tasks
 * (those with no unsatisfied dependencies) can execute concurrently.</p>
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li><b>WorkflowTask &rarr; Workflow (many-to-one)</b>:
 *       Every task belongs to exactly one workflow. {@code FetchType.LAZY}
 *       avoids loading the full workflow (and all its sibling tasks) when
 *       we only need the task itself. {@code optional = false} enforces
 *       the not-null FK constraint at the JPA level.</li>
 *
 *   <li><b>WorkflowTask &rarr; TaskDependency (one-to-many, x2)</b>:
 *       {@code incomingDependencies} lists edges where this task is the
 *       <em>dependent</em> (blocked by prerequisites).
 *       {@code outgoingDependencies} lists edges where this task is the
 *       <em>prerequisite</em> (blocking other tasks).
 *       No cascade: dependency edges are managed independently to avoid
 *       accidental deletion of edges that reference other tasks.</li>
 *
 *   <li><b>WorkflowTask &rarr; AgentExecution (one-to-many)</b>:
 *       A task may have multiple execution attempts (retries).
 *       No cascade: execution records are audit logs whose lifecycle
 *       is independent of the task entity.</li>
 *
 *   <li><b>WorkflowTask &rarr; CalendarEvent (one-to-one, optional)</b>:
 *       Only calendar-related tasks produce a CalendarEvent.
 *       {@code mappedBy = "task"} means CalendarEvent owns the FK.
 *       No cascade: the CalendarEvent has its own lifecycle (sync state,
 *       Google Calendar ID) managed by the Calendar service.</li>
 *
 *   <li><b>WorkflowTask &rarr; Approval (one-to-one, optional)</b>:
 *       Only tasks with consequential side-effects create an Approval.
 *       {@code mappedBy = "task"} means Approval owns the FK.
 *       No cascade: the Approval has its own lifecycle (pending &rarr;
 *       approved/rejected/expired) managed by the Approval Gateway.</li>
 * </ul>
 */
@Entity
@Table(name = "workflow_tasks")
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The parent workflow this task belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    /** Human-readable task name shown in the Kanban UI. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Detailed description of what this task should accomplish. */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Current lifecycle state of this task. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    /** Which agent type is responsible for executing this task. */
    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_agent", nullable = false, length = 20)
    private AgentType assignedAgent;

    /**
     * Structured JSON input for the assigned agent.
     *
     * <p>Stored as raw text at the entity layer. Deserialization into
     * agent-specific typed DTOs happens in the service/agent layer.
     * This avoids coupling the entity schema to individual agent
     * payload shapes.</p>
     */
    @Column(name = "input_payload", columnDefinition = "text")
    private String inputPayload;

    /** Structured JSON output produced by the agent after execution. */
    @Column(name = "output_payload", columnDefinition = "text")
    private String outputPayload;

    // --- Relationships (inverse / non-owning sides) ---

    @OneToMany(mappedBy = "dependentTask", fetch = FetchType.LAZY)
    private List<TaskDependency> incomingDependencies = new ArrayList<>();

    @OneToMany(mappedBy = "prerequisiteTask", fetch = FetchType.LAZY)
    private List<TaskDependency> outgoingDependencies = new ArrayList<>();

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    private List<AgentExecution> executions = new ArrayList<>();

    @OneToOne(mappedBy = "task", fetch = FetchType.LAZY)
    private CalendarEvent calendarEvent;

    @OneToOne(mappedBy = "task", fetch = FetchType.LAZY)
    private Approval approval;

    // --- Timestamps ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** No-arg constructor required by JPA. */
    protected WorkflowTask() {
    }

    /**
     * Application constructor.
     *
     * @param workflow      parent workflow
     * @param title         human-readable task name
     * @param status        initial task status (typically {@code PENDING})
     * @param assignedAgent the agent type responsible for this task
     */
    public WorkflowTask(Workflow workflow, String title, TaskStatus status, AgentType assignedAgent) {
        this.workflow = workflow;
        this.title = title;
        this.status = status;
        this.assignedAgent = assignedAgent;
    }

    // ------------------------------------------------------------------
    // JPA lifecycle callbacks
    // ------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public AgentType getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(AgentType assignedAgent) {
        this.assignedAgent = assignedAgent;
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

    public List<TaskDependency> getIncomingDependencies() {
        return incomingDependencies;
    }

    public List<TaskDependency> getOutgoingDependencies() {
        return outgoingDependencies;
    }

    public List<AgentExecution> getExecutions() {
        return executions;
    }

    public CalendarEvent getCalendarEvent() {
        return calendarEvent;
    }

    public Approval getApproval() {
        return approval;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
