package com.tradetwin.auth.dto;

import com.tradetwin.user.model.UserRole;

public class UserSummaryDto {
    private String id;
    private String email;
    private String fullName;
    private UserRole role;
    private Double reputationScore;

    public UserSummaryDto() {}

    public UserSummaryDto(String id, String email, String fullName, UserRole role, Double reputationScore) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.reputationScore = reputationScore;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String email;
        private String fullName;
        private UserRole role;
        private Double reputationScore;

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder reputationScore(Double reputationScore) { this.reputationScore = reputationScore; return this; }

        public UserSummaryDto build() {
            return new UserSummaryDto(id, email, fullName, role, reputationScore);
        }
    }

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
}
