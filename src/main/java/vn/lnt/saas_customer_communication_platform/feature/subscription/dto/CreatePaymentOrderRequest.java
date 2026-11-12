package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePaymentOrderRequest {

    @NotNull(message = "Loại yêu cầu thanh toán không được để trống (UPGRADE hoặc RENEW)")
    private String orderType; // "UPGRADE" or "RENEW"

    private Long targetPlanId; // Bắt buộc khi UPGRADE

    private Integer durationMonths = 1; // 1 hoặc 12

    private String paymentProvider = "VIETQR"; // "VIETQR", "SIMULATED", "VNPAY", "PAYOS"

    private String note;

    public CreatePaymentOrderRequest() {
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

