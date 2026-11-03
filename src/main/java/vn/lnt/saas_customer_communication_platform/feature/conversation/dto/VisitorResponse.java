package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

import java.time.LocalDateTime;

public class VisitorResponse {

    private Long id;
    private String visitorToken;
    private String name;
    private String email;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private Long companyId;

    public VisitorResponse() {
    }

    public VisitorResponse(Long id, String visitorToken, String name, String email,
                           LocalDateTime lastSeenAt, LocalDateTime createdAt, Long companyId) {
        this.id = id;
        this.visitorToken = visitorToken;
        this.name = name;
        this.email = email;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.companyId = companyId;
    }

    public static VisitorResponse fromEntity(Visitor visitor) {
        if (visitor == null) {
            return null;
        }
        return new VisitorResponse(
                visitor.getId(),
                visitor.getVisitorToken(),
                visitor.getName(),
                visitor.getEmail(),
                visitor.getLastSeenAt(),
                visitor.getCreatedAt(),
                visitor.getCompany() != null ? visitor.getCompany().getId() : null
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVisitorToken() {
        return visitorToken;
    }

    public void setVisitorToken(String visitorToken) {
        this.visitorToken = visitorToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}

