package vn.lnt.saas_customer_communication_platform.feature.company.dto;

import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;

import java.time.LocalDateTime;

public class CompanyResponse {

    private Long id;
    private String name;
    private String logoUrl;
    private String status;
    private String userRole;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CompanyResponse() {
    }

    public CompanyResponse(Long id, String name, String logoUrl, String status, String userRole, LocalDateTime joinedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.status = status;
        this.userRole = userRole;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CompanyResponse fromEntity(Company company, CompanyMemberRole userRole, LocalDateTime joinedAt) {
        return fromEntity(company, userRole != null ? userRole.name() : null, joinedAt);
    }

    public static CompanyResponse fromEntity(Company company, String userRole, LocalDateTime joinedAt) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getLogoUrl(),
                company.getStatus(),
                userRole,
                joinedAt,
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    public static CompanyResponse fromEntity(Company company) {
        return fromEntity(company, (String) null, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
