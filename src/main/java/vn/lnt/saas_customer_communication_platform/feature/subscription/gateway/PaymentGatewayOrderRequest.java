package vn.lnt.saas_customer_communication_platform.feature.subscription.gateway;

import java.math.BigDecimal;

public class PaymentGatewayOrderRequest {

    private long orderCode;
    private BigDecimal amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private String orderType;
    private String targetPlanName;

    public PaymentGatewayOrderRequest() {
    }

    public PaymentGatewayOrderRequest(long orderCode, BigDecimal amount, String description, String returnUrl, String cancelUrl, String orderType, String targetPlanName) {
        this.orderCode = orderCode;
        this.amount = amount;
        this.description = description;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
        this.orderType = orderType;
        this.targetPlanName = targetPlanName;
    }

    public long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(long orderCode) {
        this.orderCode = orderCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getTargetPlanName() {
        return targetPlanName;
    }

    public void setTargetPlanName(String targetPlanName) {
        this.targetPlanName = targetPlanName;
    }
}

