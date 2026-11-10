package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RenewSubscriptionResponse {

    private Long subscriptionId;
    private Long companyId;
    private PlanResponse plan;
    private String status;
    private LocalDateTime previousEndAt;
    private LocalDateTime newEndAt;
    private Integer durationMonths;
    private BigDecimal amountPaid;
    private String paymentProvider;
    private String transactionRef;
    private String message;

    public RenewSubscriptionResponse() {
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

    public LocalDateTime getPreviousEndAt() {
        return previousEndAt;
    }

    public void setPreviousEndAt(LocalDateTime previousEndAt) {
        this.previousEndAt = previousEndAt;
    }

    public LocalDateTime getNewEndAt() {
        return newEndAt;
    }

    public void setNewEndAt(LocalDateTime newEndAt) {
        this.newEndAt = newEndAt;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

