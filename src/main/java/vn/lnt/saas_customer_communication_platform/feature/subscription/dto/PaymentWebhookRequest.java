package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import java.math.BigDecimal;

public class PaymentWebhookRequest {

    private String transactionRef;
    private BigDecimal amount;
    private String status; // "SUCCESS", "FAILED"
    private String provider;
    private String signature;

    public PaymentWebhookRequest() {
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

