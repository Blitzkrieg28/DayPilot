package com.daypilot.api.rest;

import com.daypilot.api.dto.CreateWorkflowRequest;
import com.daypilot.api.dto.WorkflowResponse;
import com.daypilot.domain.entity.Workflow;
import com.daypilot.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow saved = workflowService.createWorkflow(request.getUserRequest());
        return mapToResponse(saved);
    }

    @GetMapping("/{id}")
    public WorkflowResponse getWorkflow(@PathVariable UUID id) {
        Workflow workflow = workflowService.getWorkflow(id);
        return mapToResponse(workflow);
    }

    private WorkflowResponse mapToResponse(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getUserRequest(),
                workflow.getStatus(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }
}
