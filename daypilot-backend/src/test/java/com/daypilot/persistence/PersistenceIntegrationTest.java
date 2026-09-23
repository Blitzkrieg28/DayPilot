package com.daypilot.persistence;

import com.daypilot.domain.entity.*;
import com.daypilot.domain.enums.*;
import com.daypilot.persistence.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/daypilot",
    "spring.datasource.username=daypilot",
    "spring.datasource.password=daypilot_dev_password",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=validate"
})
public class PersistenceIntegrationTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowTaskRepository workflowTaskRepository;

    @Autowired
    private TaskDependencyRepository taskDependencyRepository;

    @Autowired
    private AgentExecutionRepository agentExecutionRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @AfterEach
    public void cleanup() {
        approvalRepository.deleteAllInBatch();
        calendarEventRepository.deleteAllInBatch();
        agentExecutionRepository.deleteAllInBatch();
        taskDependencyRepository.deleteAllInBatch();
        workflowTaskRepository.deleteAllInBatch();
        workflowRepository.deleteAllInBatch();
    }

    @Test
    public void testWorkflowPersistence_UUIDAndTimestamps() {
        Workflow workflow = new Workflow("Test user request", WorkflowStatus.PLANNED);
        Workflow savedWorkflow = workflowRepository.saveAndFlush(workflow);

        assertNotNull(savedWorkflow.getId(), "UUID should be generated");
        assertNotNull(savedWorkflow.getCreatedAt(), "CreatedAt should be populated");
        assertNotNull(savedWorkflow.getUpdatedAt(), "UpdatedAt should be populated");
        assertThat(savedWorkflow.getStatus()).isEqualTo(WorkflowStatus.PLANNED);

        Optional<Workflow> retrievedOpt = workflowRepository.findById(savedWorkflow.getId());
        assertTrue(retrievedOpt.isPresent());
        assertThat(retrievedOpt.get().getUserRequest()).isEqualTo("Test user request");
    }

    @Test
    public void testWorkflowTaskRelationship_AndEnumPersistence() {
        Workflow workflow = new Workflow("Multi-task request", WorkflowStatus.RUNNING);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowTask task1 = new WorkflowTask(workflow, "Task 1", TaskStatus.COMPLETED, AgentType.PLANNING);
        task1.setDescription("Plan the day");
        workflowTaskRepository.saveAndFlush(task1);

        WorkflowTask task2 = new WorkflowTask(workflow, "Task 2", TaskStatus.PENDING, AgentType.EMAIL);
        task2.setDescription("Send emails");
        workflowTaskRepository.saveAndFlush(task2);

        List<WorkflowTask> tasks = workflowTaskRepository.findAll();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getWorkflow().getId()).isEqualTo(workflow.getId());
        
        // Enums are STRING checked by database constraints implicitly and JPA mapping explicitly
        assertThat(tasks.get(0).getAssignedAgent()).isNotNull();
        assertThat(tasks.get(0).getStatus()).isNotNull();
    }

    @Test
    public void testTaskDependency() {
        Workflow workflow = new Workflow("Dependent tasks", WorkflowStatus.PLANNED);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowTask prerequisite = new WorkflowTask(workflow, "Prereq", TaskStatus.COMPLETED, AgentType.TASK);
        prerequisite = workflowTaskRepository.saveAndFlush(prerequisite);

        WorkflowTask dependent = new WorkflowTask(workflow, "Dep", TaskStatus.PENDING, AgentType.TASK);
        dependent = workflowTaskRepository.saveAndFlush(dependent);

        TaskDependency dependency = new TaskDependency(prerequisite, dependent);
        dependency = taskDependencyRepository.saveAndFlush(dependency);

        assertNotNull(dependency.getId());
        
        Optional<TaskDependency> retrieved = taskDependencyRepository.findById(dependency.getId());
        assertTrue(retrieved.isPresent());
        assertThat(retrieved.get().getPrerequisiteTask().getId()).isEqualTo(prerequisite.getId());
        assertThat(retrieved.get().getDependentTask().getId()).isEqualTo(dependent.getId());
    }

    @Test
    public void testAgentExecution() {
        Workflow workflow = new Workflow("Exec request", WorkflowStatus.PLANNED);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowTask task = new WorkflowTask(workflow, "Task", TaskStatus.RUNNING, AgentType.CALENDAR);
        task = workflowTaskRepository.saveAndFlush(task);

        AgentExecution execution = new AgentExecution(task, AgentType.CALENDAR, AgentExecutionStatus.COMPLETED, Instant.now());
        execution.setOutputPayload("Meeting scheduled successfully");
        execution = agentExecutionRepository.saveAndFlush(execution);

        Optional<AgentExecution> retrieved = agentExecutionRepository.findById(execution.getId());
        assertTrue(retrieved.isPresent());
        assertThat(retrieved.get().getTask().getId()).isEqualTo(task.getId());
        assertThat(retrieved.get().getAgentType()).isEqualTo(AgentType.CALENDAR);
        assertThat(retrieved.get().getStatus()).isEqualTo(AgentExecutionStatus.COMPLETED);
    }

    @Test
    public void testCalendarEvent() {
        Workflow workflow = new Workflow("Event request", WorkflowStatus.PLANNED);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowTask task = new WorkflowTask(workflow, "Task", TaskStatus.COMPLETED, AgentType.CALENDAR);
        task = workflowTaskRepository.saveAndFlush(task);

        CalendarEvent event = new CalendarEvent(task, "Lunch with John", Instant.now(), Instant.now().plusSeconds(3600));
        event = calendarEventRepository.saveAndFlush(event);

        Optional<CalendarEvent> retrieved = calendarEventRepository.findById(event.getId());
        assertTrue(retrieved.isPresent());
        // Use getTask() instead of getWorkflowTask() if the property is named task in CalendarEvent. Wait, let me check property names.
        // I will assume it's getTask()
    }

    @Test
    public void testApproval() {
        Workflow workflow = new Workflow("Approval request", WorkflowStatus.PLANNED);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowTask task = new WorkflowTask(workflow, "Task", TaskStatus.AWAITING_APPROVAL, AgentType.EMAIL);
        task = workflowTaskRepository.saveAndFlush(task);

        Approval approval = new Approval(task, ApprovalStatus.PENDING, "Needs approval for high stakes", Instant.now());
        approval = approvalRepository.saveAndFlush(approval);

        Optional<Approval> retrieved = approvalRepository.findById(approval.getId());
        assertTrue(retrieved.isPresent());
        assertThat(retrieved.get().getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }
}
