package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSWebhookPayload {

    private String code;
    private String desc;
    private PayOSWebhookData data;
    private String signature;

    public PayOSWebhookPayload() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public PayOSWebhookData getData() {
        return data;
    }

    public void setData(PayOSWebhookData data) {
        this.data = data;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignatureData() {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (data.getAmount() != null) sb.append("amount=").append(data.getAmount()).append("&");
        if (data.getDescription() != null) sb.append("description=").append(data.getDescription()).append("&");
        if (data.getOrderCode() != null) sb.append("orderCode=").append(data.getOrderCode()).append("&");
        if (data.getReference() != null) sb.append("reference=").append(data.getReference());
        String str = sb.toString();
        if (str.endsWith("&")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayOSWebhookData {
        private Long orderCode;
        private Long amount;
        private String description;
        private String accountNumber;
        private String reference;
        private String transactionDateTime;
        private String currency;
        private String paymentLinkId;
        private String code;
        private String desc;

        public PayOSWebhookData() {
        }

        public Long getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(Long orderCode) {
            this.orderCode = orderCode;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getTransactionDateTime() {
            return transactionDateTime;
        }

        public void setTransactionDateTime(String transactionDateTime) {
            this.transactionDateTime = transactionDateTime;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getPaymentLinkId() {
            return paymentLinkId;
        }

        public void setPaymentLinkId(String paymentLinkId) {
            this.paymentLinkId = paymentLinkId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }
}

