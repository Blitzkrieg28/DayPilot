package com.daypilot.api.dto;

import com.daypilot.domain.enums.WorkflowStatus;

import java.time.Instant;
import java.util.UUID;

public class WorkflowResponse {
    private UUID id;
    private String userRequest;
    private WorkflowStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public WorkflowResponse() {}

    public WorkflowResponse(UUID id, String userRequest, WorkflowStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userRequest = userRequest;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
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
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
