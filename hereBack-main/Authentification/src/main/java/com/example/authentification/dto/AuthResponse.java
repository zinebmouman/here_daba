
// AuthResponse.java
package com.example.authentification.dto;

import java.util.Map;

public class AuthResponse {
    private boolean success;
    private String message;
    private String uid;
    private String email;
    private String displayName;
    private String role;
    private String idToken;
    private Map<String, Boolean> roles;
    private Long userId; // ID PostgreSQL

    // Constructeurs
    public AuthResponse() {}

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public Map<String, Boolean> getRoles() { return roles; }
    public void setRoles(Map<String, Boolean> roles) { this.roles = roles; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}