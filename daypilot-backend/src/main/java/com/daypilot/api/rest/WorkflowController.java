package com.daypilot.api.rest;

import com.daypilot.api.dto.CreateWorkflowRequest;
import com.daypilot.api.dto.WorkflowResponse;
import com.daypilot.domain.entity.Workflow;
import com.daypilot.domain.enums.WorkflowStatus;
import com.daypilot.persistence.repository.WorkflowRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    public WorkflowController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow workflow = new Workflow(request.getUserRequest(), WorkflowStatus.PLANNED);
        Workflow saved = workflowRepository.save(workflow);
        return mapToResponse(saved);
    }

    @GetMapping("/{id}")
    public WorkflowResponse getWorkflow(@PathVariable UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
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
