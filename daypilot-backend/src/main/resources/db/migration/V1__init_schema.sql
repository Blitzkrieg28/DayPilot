-- V1__init_schema.sql
-- Initializes the Phase 1.1 Core Domain Model schema.

CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    user_request TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE workflow_tasks (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    assigned_agent VARCHAR(20) NOT NULL,
    input_payload TEXT,
    output_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_workflow_tasks_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id)
);

CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY,
    prerequisite_task_id UUID NOT NULL,
    dependent_task_id UUID NOT NULL,
    CONSTRAINT fk_task_dependencies_prereq FOREIGN KEY (prerequisite_task_id) REFERENCES workflow_tasks(id),
    CONSTRAINT fk_task_dependencies_depend FOREIGN KEY (dependent_task_id) REFERENCES workflow_tasks(id)
);

CREATE TABLE agent_executions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    agent_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    input_payload TEXT,
    output_payload TEXT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_agent_executions_task FOREIGN KEY (task_id) REFERENCES workflow_tasks(id)
);

CREATE TABLE calendar_events (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    location VARCHAR(255),
    google_event_id VARCHAR(255),
    synced BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_calendar_events_task FOREIGN KEY (task_id) REFERENCES workflow_tasks(id)
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    request_summary TEXT NOT NULL,
    resolved_by VARCHAR(255),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_approvals_task FOREIGN KEY (task_id) REFERENCES workflow_tasks(id)
);

-- Sensible indexes for the orchestrator
CREATE INDEX idx_workflow_tasks_workflow_id ON workflow_tasks(workflow_id);
CREATE INDEX idx_workflow_tasks_status ON workflow_tasks(status);
CREATE INDEX idx_workflow_tasks_assigned_agent ON workflow_tasks(assigned_agent);
CREATE INDEX idx_task_dependencies_prerequisite ON task_dependencies(prerequisite_task_id);
CREATE INDEX idx_task_dependencies_dependent ON task_dependencies(dependent_task_id);
CREATE INDEX idx_agent_executions_task_id ON agent_executions(task_id);
