package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import java.time.LocalDateTime;

public class DowngradeSubscriptionResponse {

    private Long subscriptionId;
    private Long companyId;
    private PlanResponse currentPlan;
    private PlanResponse pendingPlan;
    private LocalDateTime effectiveDate;
    private String message;

    public DowngradeSubscriptionResponse() {
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public PlanResponse getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(PlanResponse currentPlan) {
        this.currentPlan = currentPlan;
    }

    public PlanResponse getPendingPlan() {
        return pendingPlan;
    }

    public void setPendingPlan(PlanResponse pendingPlan) {
        this.pendingPlan = pendingPlan;
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

