package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotNull;

public class RenewSubscriptionRequest {

    @NotNull(message = "Chu kỳ gia hạn không được để trống")
    private Integer durationMonths = 1;

    private String paymentProvider = "SIMULATED";

    public RenewSubscriptionRequest() {
    }

    public RenewSubscriptionRequest(Integer durationMonths, String paymentProvider) {
        this.durationMonths = durationMonths;
        this.paymentProvider = paymentProvider;
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

