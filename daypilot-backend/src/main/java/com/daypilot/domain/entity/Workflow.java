package com.daypilot.domain.entity;

import com.daypilot.domain.enums.WorkflowStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for every user request.
 *
 * <p>A single natural-language request from the user becomes one {@code Workflow}.
 * The Planning Agent decomposes it into a directed acyclic graph (DAG) of
 * {@link WorkflowTask}s. The orchestrator drives the workflow through its
 * lifecycle states while the tasks execute concurrently where the DAG allows.</p>
 *
 * <h3>Relationship: Workflow to WorkflowTask (one-to-many)</h3>
 * <ul>
 *   <li><b>cascade = ALL</b> &mdash; Workflow is the root aggregate that owns its
 *       tasks. Persisting, merging, refreshing, or removing a workflow propagates
 *       to its tasks. This is the standard DDD aggregate-root pattern: the root
 *       controls the lifecycle of its children.</li>
 *   <li><b>fetch = LAZY</b> &mdash; Loading a workflow does not automatically load
 *       every task. The orchestrator can query workflows by status without
 *       materialising the full task graph in memory.</li>
 *   <li><b>orphanRemoval is intentionally omitted</b> &mdash; We avoid destructive
 *       cascade semantics unless a future service layer explicitly needs them.
 *       Task removal will be handled by explicit repository calls, not by
 *       collection mutation.</li>
 * </ul>
 */
@Entity
@Table(name = "workflows")
public class Workflow {

    /**
     * Surrogate primary key.
     *
     * <p>UUID is used instead of a database sequence for three reasons:
     * (1) no sequential-ID guessing in REST APIs,
     * (2) safe for distributed ID generation without a central coordinator,
     * (3) IDs can be assigned before the entity is persisted.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The original, unstructured natural-language request from the user.
     *
     * <p>Stored as TEXT because user requests are arbitrarily long.
     * This value is immutable after creation; the Planning Agent reads it
     * to produce the task DAG.</p>
     */
    @Column(name = "user_request", nullable = false, columnDefinition = "text")
    private String userRequest;

    /**
     * Current lifecycle state.
     *
     * <p>Stored as a string ({@code EnumType.STRING}) so the database column
     * contains human-readable values like {@code "RUNNING"} rather than
     * fragile ordinal integers. This is safe against enum reordering.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status;

    /**
     * The tasks that make up this workflow's DAG.
     *
     * @see WorkflowTask
     */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowTask> tasks = new ArrayList<>();

    /**
     * When this workflow was first persisted.
     *
     * <p>{@link Instant} is used instead of {@link java.time.LocalDateTime}
     * because Instant represents an unambiguous point on the UTC timeline.
     * Time-zone conversion is a presentation concern, not a storage concern.</p>
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When this workflow was last modified. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /**
     * No-arg constructor required by JPA.
     *
     * <p>{@code protected} visibility prevents application code from creating
     * uninitialized entities while still allowing the JPA provider (Hibernate)
     * to instantiate them via reflection.</p>
     */
    protected Workflow() {
    }

    /**
     * Application constructor for creating a new workflow.
     *
     * @param userRequest the original user request text
     * @param status      initial workflow status (typically {@code PLANNED})
     */
    public Workflow(String userRequest, WorkflowStatus status) {
        this.userRequest = userRequest;
        this.status = status;
    }

    // ------------------------------------------------------------------
    // JPA lifecycle callbacks
    // ------------------------------------------------------------------

    /**
     * Sets creation and modification timestamps just before the first INSERT.
     *
     * <p>Using {@code @PrePersist} keeps timestamp logic in the entity itself
     * so that every code path that persists a workflow gets consistent timestamps
     * without relying on service-layer discipline.</p>
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Refreshes the modification timestamp just before every UPDATE.
     */
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

    public String getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(String userRequest) {
        this.userRequest = userRequest;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<WorkflowTask> getTasks() {
        return tasks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
