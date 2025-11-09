package ru.netology.filestorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {

    @JsonProperty("auth-token")
    private String authToken;

    public AuthResponse(String authToken) {
        this.authToken = authToken;
    }

    public AuthResponse() {
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}