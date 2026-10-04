package vn.lnt.saas_customer_communication_platform.feature.auth.dto;

import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Long currentCompanyId;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String fullName, String role, Long currentCompanyId, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.currentCompanyId = currentCompanyId;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCurrentCompanyId(),
                user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCurrentCompanyId() {
        return currentCompanyId;
    }

    public void setCurrentCompanyId(Long currentCompanyId) {
        this.currentCompanyId = currentCompanyId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
