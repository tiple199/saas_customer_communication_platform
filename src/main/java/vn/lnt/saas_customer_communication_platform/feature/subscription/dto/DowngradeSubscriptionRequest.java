package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotNull;

public class DowngradeSubscriptionRequest {

    @NotNull(message = "ID gói dịch vụ muốn hạ cấp không được để trống")
    private Long targetPlanId;

    // Lý do hạ cấp (tùy chọn)
    private String reason;

    public DowngradeSubscriptionRequest() {
    }

    public DowngradeSubscriptionRequest(Long targetPlanId, String reason) {
        this.targetPlanId = targetPlanId;
        this.reason = reason;
    }

    public Long getTargetPlanId() {
        return targetPlanId;
    }

    public void setTargetPlanId(Long targetPlanId) {
        this.targetPlanId = targetPlanId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

