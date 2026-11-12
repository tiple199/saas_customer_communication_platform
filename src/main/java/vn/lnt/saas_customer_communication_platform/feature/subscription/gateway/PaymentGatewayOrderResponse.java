package vn.lnt.saas_customer_communication_platform.feature.subscription.gateway;

import java.math.BigDecimal;

public class PaymentGatewayOrderResponse {

    private long orderCode;
    private String checkoutUrl;
    private String qrCodeUrl;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private BigDecimal amount;
    private String description;
    private boolean realGateway;

    public PaymentGatewayOrderResponse() {
    }

    public long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(long orderCode) {
        this.orderCode = orderCode;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public boolean isRealGateway() {
        return realGateway;
    }

    public void setRealGateway(boolean realGateway) {
        this.realGateway = realGateway;
    }
}

