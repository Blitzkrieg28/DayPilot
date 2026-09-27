package com.daypilot.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateWorkflowRequest {
    @NotBlank(message = "User request must not be blank")
    private String userRequest;

    public String getUserRequest() {
        return userRequest;
    }

    public void setUserRequest(String userRequest) {
        this.userRequest = userRequest;
    }
}
