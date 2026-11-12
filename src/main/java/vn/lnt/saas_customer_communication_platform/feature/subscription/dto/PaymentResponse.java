package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private Long subscriptionId;
    private Long companyId;
    private String companyName;
    private String planName;
    private String orderType;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String status;
    private String transactionRef;
    private String description;
    private Integer durationMonths;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public PaymentResponse() {
    }

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentResponse resp = new PaymentResponse();
        resp.setId(payment.getId());
        if (payment.getSubscription() != null) {
            resp.setSubscriptionId(payment.getSubscription().getId());
            if (payment.getSubscription().getCompany() != null) {
                resp.setCompanyId(payment.getSubscription().getCompany().getId());
                resp.setCompanyName(payment.getSubscription().getCompany().getName());
            }
            if (payment.getSubscription().getPlan() != null) {
                resp.setPlanName(payment.getSubscription().getPlan().getName());
            }
        }
        String ref = payment.getTransactionRef() != null ? payment.getTransactionRef() : "";
        String orderType = ref.startsWith("RNW") ? "RENEW" : "UPGRADE";
        int durationMonths = ref.contains("_12M_") ? 12 : 1;

        resp.setOrderType(orderType);
        resp.setDurationMonths(durationMonths);
        resp.setAmount(payment.getAmount());
        resp.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "VND");
        resp.setProvider(payment.getProvider());
        resp.setStatus(payment.getStatus());
        resp.setTransactionRef(payment.getTransactionRef());
        resp.setDescription("RENEW".equals(orderType) ? "Gia hạn gói dịch vụ (" + durationMonths + " tháng)" : "Nâng cấp gói dịch vụ (" + durationMonths + " tháng)");
        resp.setCreatedAt(payment.getCreatedAt());
        resp.setPaidAt(payment.getPaidAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}

