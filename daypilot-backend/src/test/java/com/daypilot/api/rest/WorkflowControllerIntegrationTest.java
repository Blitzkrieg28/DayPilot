package com.daypilot.api.rest;

import com.daypilot.api.dto.CreateWorkflowRequest;
import com.daypilot.api.dto.WorkflowResponse;
import com.daypilot.domain.entity.Workflow;
import com.daypilot.domain.enums.WorkflowStatus;
import com.daypilot.persistence.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/daypilot",
    "spring.datasource.username=daypilot",
    "spring.datasource.password=daypilot_dev_password",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=validate"
})
public class WorkflowControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WorkflowRepository workflowRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    public void cleanup() {
        workflowRepository.deleteAllInBatch();
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/workflows";
    }

    @Test
    public void testCreateWorkflow_Success() throws Exception {
        CreateWorkflowRequest requestDto = new CreateWorkflowRequest();
        requestDto.setUserRequest("Schedule a meeting with John tomorrow at 2pm");
        String requestBody = objectMapper.writeValueAsString(requestDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        
        // Ensure Jackson knows to deserialize Instant correctly if java time module is registered
        // but for safety we can just parse the response dynamically or ensure we register modules
        // Actually ObjectMapper might need JavaTimeModule, but for simple assertion we can just read tree
        var jsonNode = objectMapper.readTree(response.body());
        
        assertThat(jsonNode.has("id")).isTrue();
        assertThat(jsonNode.get("userRequest").asText()).isEqualTo("Schedule a meeting with John tomorrow at 2pm");
        assertThat(jsonNode.get("status").asText()).isEqualTo("PLANNED");
        assertThat(jsonNode.has("createdAt")).isTrue();
        assertThat(jsonNode.has("updatedAt")).isTrue();
        
        UUID savedId = UUID.fromString(jsonNode.get("id").asText());
        Optional<Workflow> savedWorkflow = workflowRepository.findById(savedId);
        assertThat(savedWorkflow).isPresent();
        assertThat(savedWorkflow.get().getUserRequest()).isEqualTo("Schedule a meeting with John tomorrow at 2pm");
        assertThat(savedWorkflow.get().getStatus()).isEqualTo(WorkflowStatus.PLANNED);
    }

    @Test
    public void testCreateWorkflow_ValidationFailure() throws Exception {
        CreateWorkflowRequest requestDto = new CreateWorkflowRequest();
        requestDto.setUserRequest("   "); // Blank user request
        String requestBody = objectMapper.writeValueAsString(requestDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(workflowRepository.count()).isEqualTo(0);
    }

    @Test
    public void testGetWorkflow_Success() throws Exception {
        Workflow workflow = new Workflow("Find a good restaurant for dinner", WorkflowStatus.PLANNED);
        workflow = workflowRepository.saveAndFlush(workflow);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/" + workflow.getId()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        
        var jsonNode = objectMapper.readTree(response.body());
        assertThat(jsonNode.get("id").asText()).isEqualTo(workflow.getId().toString());
        assertThat(jsonNode.get("userRequest").asText()).isEqualTo("Find a good restaurant for dinner");
        assertThat(jsonNode.get("status").asText()).isEqualTo("PLANNED");
        assertThat(jsonNode.has("createdAt")).isTrue();
        assertThat(jsonNode.has("updatedAt")).isTrue();
    }

    @Test
    public void testGetWorkflow_NotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/" + UUID.randomUUID()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
