package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfirmPaymentRequest {

    @NotBlank(message = "Mã giao dịch (transactionRef) không được để trống")
    private String transactionRef;

    private String note;

    public ConfirmPaymentRequest() {
    }

    public ConfirmPaymentRequest(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

