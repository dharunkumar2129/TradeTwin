package com.tradetwin.auth.dto;

import com.tradetwin.user.model.UserRole;

public class AuthResponse {
    private String token;
    private String id;
    private String email;
    private String fullName;
    private UserRole role;
    private Double reputationScore;
    private String defaultCurrency = "USD";

    public AuthResponse() {}

    public AuthResponse(String token, String id, String email, String fullName, UserRole role, Double reputationScore, String defaultCurrency) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.reputationScore = reputationScore;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency : "USD";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String token;
        private String id;
        private String email;
        private String fullName;
        private UserRole role;
        private Double reputationScore;
        private String defaultCurrency = "USD";

        public Builder token(String token) { this.token = token; return this; }
        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder reputationScore(Double reputationScore) { this.reputationScore = reputationScore; return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, id, email, fullName, role, reputationScore, defaultCurrency);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
