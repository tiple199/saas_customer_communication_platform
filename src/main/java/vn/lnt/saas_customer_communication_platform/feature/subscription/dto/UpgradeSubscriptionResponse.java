package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpgradeSubscriptionResponse {

    private Long subscriptionId;
    private Long companyId;
    private PlanResponse previousPlan;
    private PlanResponse upgradedPlan;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BigDecimal amountPaid;
    private BigDecimal fullPrice;
    private BigDecimal unusedCreditAmount;
    private Long remainingDaysCredited;
    private String paymentProvider;
    private String transactionRef;
    private String message;

    public UpgradeSubscriptionResponse() {
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

    public PlanResponse getPreviousPlan() {
        return previousPlan;
    }

    public void setPreviousPlan(PlanResponse previousPlan) {
        this.previousPlan = previousPlan;
    }

    public PlanResponse getUpgradedPlan() {
        return upgradedPlan;
    }

    public void setUpgradedPlan(PlanResponse upgradedPlan) {
        this.upgradedPlan = upgradedPlan;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getFullPrice() {
        return fullPrice;
    }

    public void setFullPrice(BigDecimal fullPrice) {
        this.fullPrice = fullPrice;
    }

    public BigDecimal getUnusedCreditAmount() {
        return unusedCreditAmount;
    }

    public void setUnusedCreditAmount(BigDecimal unusedCreditAmount) {
        this.unusedCreditAmount = unusedCreditAmount;
    }

    public Long getRemainingDaysCredited() {
        return remainingDaysCredited;
    }

    public void setRemainingDaysCredited(Long remainingDaysCredited) {
        this.remainingDaysCredited = remainingDaysCredited;
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

