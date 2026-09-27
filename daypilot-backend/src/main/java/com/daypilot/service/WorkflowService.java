package com.daypilot.service;

import com.daypilot.domain.entity.Workflow;
import com.daypilot.domain.enums.WorkflowStatus;
import com.daypilot.persistence.repository.WorkflowRepository;
import com.daypilot.service.exception.WorkflowNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public Workflow createWorkflow(String userRequest) {
        Workflow workflow = new Workflow(userRequest, WorkflowStatus.PLANNED);
        return workflowRepository.save(workflow);
    }

    @Transactional(readOnly = true)
    public Workflow getWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
    }
}
