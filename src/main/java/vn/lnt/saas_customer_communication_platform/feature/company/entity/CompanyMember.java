package vn.lnt.saas_customer_communication_platform.feature.company.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_members")
public class CompanyMember {

    @EmbeddedId
    private CompanyMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private CompanyMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CompanyMemberStatus status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public CompanyMember() {
    }

    public CompanyMember(CompanyMemberId id, CompanyMemberRole role, CompanyMemberStatus status) {
        this.id = id;
        this.role = role;
        this.status = status;
    }

    public CompanyMember(Long userId, Long companyId, CompanyMemberRole role, CompanyMemberStatus status) {
        this.id = new CompanyMemberId(userId, companyId);
        this.role = role;
        this.status = status;
    }

    public CompanyMemberId getId() {
        return id;
    }

    public void setId(CompanyMemberId id) {
        this.id = id;
    }

    public CompanyMemberRole getRole() {
        return role;
    }

    public void setRole(CompanyMemberRole role) {
        this.role = role;
    }

    public CompanyMemberStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyMemberStatus status) {
        this.status = status;
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
