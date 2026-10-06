package vn.lnt.saas_customer_communication_platform.feature.company.dto;

import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;

import java.time.LocalDateTime;

public class CompanyMemberResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private CompanyMemberRole role;
    private CompanyMemberStatus status;
    private LocalDateTime joinedAt;

    public CompanyMemberResponse() {
    }

    public CompanyMemberResponse(Long userId, String email, String fullName, String avatarUrl,
                                CompanyMemberRole role, CompanyMemberStatus status, LocalDateTime joinedAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public static CompanyMemberResponse fromEntity(CompanyMember member, User user) {
        return new CompanyMemberResponse(
                member.getId().getUserId(),
                user != null ? user.getEmail() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getAvatarUrl() : null,
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
}
