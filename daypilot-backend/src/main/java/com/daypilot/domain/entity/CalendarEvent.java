package com.daypilot.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A calendar event proposed or created by the Calendar Agent.
 *
 * <p>Tracks the full lifecycle from proposal through optional approval
 * to synchronisation with Google Calendar. The {@code synced} flag and
 * {@code googleEventId} together represent whether the event exists
 * only in DayPilot's database or has been pushed to the external calendar.</p>
 *
 * <h3>Relationship: CalendarEvent to WorkflowTask (one-to-one)</h3>
 * <ul>
 *   <li><b>Owning side</b> &mdash; CalendarEvent holds the {@code task_id}
 *       foreign key because it is the optional child: a WorkflowTask may or
 *       may not produce a CalendarEvent. Placing the FK here avoids a nullable
 *       column on the workflow_tasks table.</li>
 *   <li><b>fetch = LAZY</b> &mdash; Querying a CalendarEvent (e.g. for
 *       Google Calendar sync) should not force-load the full WorkflowTask
 *       and its transitive graph.</li>
 *   <li><b>optional = false</b> &mdash; Every CalendarEvent must be linked
 *       to exactly one task.</li>
 *   <li><b>unique = true on JoinColumn</b> &mdash; Enforces the one-to-one
 *       constraint at the database level: no two CalendarEvents can reference
 *       the same task.</li>
 *   <li><b>No cascade</b> &mdash; The CalendarEvent has its own lifecycle
 *       (sync state, Google event ID) managed by the Calendar service,
 *       independent of task persistence operations.</li>
 * </ul>
 */
@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The task that produced this calendar event. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, unique = true)
    private WorkflowTask task;

    /** Event title as it appears on the calendar. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Detailed event description. */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Event start time (UTC). */
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    /** Event end time (UTC). */
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    /** Physical or virtual location for the event. */
    @Column(name = "location")
    private String location;

    /**
     * Google Calendar event ID, populated after successful sync.
     * Null before the event has been pushed to Google.
     */
    @Column(name = "google_event_id")
    private String googleEventId;

    /** Whether the event has been synchronised to Google Calendar. */
    @Column(name = "synced", nullable = false)
    private boolean synced = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------

    /** No-arg constructor required by JPA. */
    protected CalendarEvent() {
    }

    /**
     * Application constructor for creating a proposed calendar event.
     *
     * @param task      the task that produced this event
     * @param title     event title
     * @param startTime event start (UTC)
     * @param endTime   event end (UTC)
     */
    public CalendarEvent(WorkflowTask task, String title,
                         Instant startTime, Instant endTime) {
        this.task = task;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public WorkflowTask getTask() {
        return task;
    }

    public void setTask(WorkflowTask task) {
        this.task = task;
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

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
