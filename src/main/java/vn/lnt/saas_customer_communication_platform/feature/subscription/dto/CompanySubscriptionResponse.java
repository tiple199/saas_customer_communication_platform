package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;

import java.time.LocalDateTime;

public class CompanySubscriptionResponse {

    private Long id;
    private Long companyId;
    private PlanResponse plan;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long pendingPlanId;
    private PlanResponse pendingPlan;

    public CompanySubscriptionResponse() {
    }

    public static CompanySubscriptionResponse fromEntity(Subscription subscription, PlanResponse pendingPlan) {
        if (subscription == null) {
            return null;
        }
        CompanySubscriptionResponse resp = new CompanySubscriptionResponse();
        resp.setId(subscription.getId());
        resp.setCompanyId(subscription.getCompany() != null ? subscription.getCompany().getId() : null);
        resp.setPlan(PlanResponse.fromEntity(subscription.getPlan()));
        resp.setStatus(subscription.getStatus());
        resp.setStartAt(subscription.getStartAt());
        resp.setEndAt(subscription.getEndAt());
        resp.setPendingPlanId(subscription.getPendingPlanId());
        resp.setPendingPlan(pendingPlan);
        return resp;
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

    public PlanResponse getPlan() {
        return plan;
    }

    public void setPlan(PlanResponse plan) {
        this.plan = plan;
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

    public Long getPendingPlanId() {
        return pendingPlanId;
    }

    public void setPendingPlanId(Long pendingPlanId) {
        this.pendingPlanId = pendingPlanId;
    }

    public PlanResponse getPendingPlan() {
        return pendingPlan;
    }

    public void setPendingPlan(PlanResponse pendingPlan) {
        this.pendingPlan = pendingPlan;
    }
}

