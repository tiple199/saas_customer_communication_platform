package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotNull;

public class UpgradeSubscriptionRequest {

    @NotNull(message = "ID gói dịch vụ muốn nâng cấp không được để trống")
    private Long targetPlanId;

    // Thời hạn đăng ký theo tháng (1, 3, 6, 12 tháng), mặc định 1 tháng
    private Integer durationMonths = 1;

    // Phương thức thanh toán giả lập hoặc cổng thanh toán: "SIMULATED", "VNPAY", "MOMO", "BANK_TRANSFER"
    private String paymentProvider = "SIMULATED";

    public UpgradeSubscriptionRequest() {
    }

    public UpgradeSubscriptionRequest(Long targetPlanId, Integer durationMonths, String paymentProvider) {
        this.targetPlanId = targetPlanId;
        this.durationMonths = durationMonths != null ? durationMonths : 1;
        this.paymentProvider = paymentProvider != null ? paymentProvider : "SIMULATED";
    }

    public Long getTargetPlanId() {
        return targetPlanId;
    }

    public void setTargetPlanId(Long targetPlanId) {
        this.targetPlanId = targetPlanId;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
}

