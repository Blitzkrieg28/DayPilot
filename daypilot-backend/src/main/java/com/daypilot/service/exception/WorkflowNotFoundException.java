package com.daypilot.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkflowNotFoundException extends RuntimeException {
    private final UUID workflowId;

    public WorkflowNotFoundException(UUID workflowId) {
        super("Workflow not found with ID: " + workflowId);
        this.workflowId = workflowId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }
}
