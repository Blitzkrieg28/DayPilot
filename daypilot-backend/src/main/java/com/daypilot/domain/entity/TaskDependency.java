package com.daypilot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * An edge in the workflow DAG linking a prerequisite task to a dependent task.
 *
 * <p>This is modelled as a dedicated entity rather than a {@code @ManyToMany}
 * join for two reasons:</p>
 * <ol>
 *   <li>The DAG structure is a first-class domain concept that the orchestrator
 *       queries directly (e.g. "find all tasks whose prerequisites are complete").
 *       A named entity makes these queries natural.</li>
 *   <li>The edge will eventually carry metadata such as dependency type
 *       (strict vs. optional), conditions, or timeout policies.</li>
 * </ol>
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li><b>prerequisiteTask (many-to-one)</b>: The task that must complete
 *       before the dependent task can start. {@code FetchType.LAZY} avoids
 *       loading the full task graph when only inspecting the dependency edge.
 *       {@code optional = false} because every edge must have both endpoints.</li>
 *   <li><b>dependentTask (many-to-one)</b>: The task that is blocked until
 *       the prerequisite completes. Same fetch/optional semantics.</li>
 * </ul>
 *
 * <p>No cascade in either direction: deleting a dependency edge should not
 * delete the tasks it connects, and deleting a task does not automatically
 * cascade to edges (which would require careful handling of the opposite
 * task's collection).</p>
 */
@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The task that must complete first. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_task_id", nullable = false)
    private WorkflowTask prerequisiteTask;

    /** The task that is blocked until the prerequisite completes. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_task_id", nullable = false)
    private WorkflowTask dependentTask;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** No-arg constructor required by JPA. */
    protected TaskDependency() {
    }

    /**
     * Application constructor.
     *
     * @param prerequisiteTask the task that must finish first
     * @param dependentTask    the task that waits
     */
    public TaskDependency(WorkflowTask prerequisiteTask, WorkflowTask dependentTask) {
        this.prerequisiteTask = prerequisiteTask;
        this.dependentTask = dependentTask;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public WorkflowTask getPrerequisiteTask() {
        return prerequisiteTask;
    }

    public void setPrerequisiteTask(WorkflowTask prerequisiteTask) {
        this.prerequisiteTask = prerequisiteTask;
    }

    public WorkflowTask getDependentTask() {
        return dependentTask;
    }

    public void setDependentTask(WorkflowTask dependentTask) {
        this.dependentTask = dependentTask;
    }
}
