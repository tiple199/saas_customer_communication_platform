package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SuperAdminSubscriptionItemDto {

    private Long id;
    private Long companyId;
    private String companyName;
    private String companyStatus;
    private String ownerName;
    private String ownerEmail;

    private Long planId;
    private String planName;
    private BigDecimal planPrice;

    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean isLifetime;
    private Long daysRemaining;

    private Long pendingPlanId;
    private String pendingPlanName;

    private LocalDateTime createdAt;

    public SuperAdminSubscriptionItemDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyStatus() {
        return companyStatus;
    }

    public void setCompanyStatus(String companyStatus) {
        this.companyStatus = companyStatus;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getPlanPrice() {
        return planPrice;
    }

    public void setPlanPrice(BigDecimal planPrice) {
        this.planPrice = planPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public boolean isLifetime() {
        return isLifetime;
    }

    public void setLifetime(boolean lifetime) {
        isLifetime = lifetime;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public Long getPendingPlanId() {
        return pendingPlanId;
    }

    public void setPendingPlanId(Long pendingPlanId) {
        this.pendingPlanId = pendingPlanId;
    }

    public String getPendingPlanName() {
        return pendingPlanName;
    }

    public void setPendingPlanName(String pendingPlanName) {
        this.pendingPlanName = pendingPlanName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

