package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCompanyStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    private String reason;

    public UpdateCompanyStatusRequest() {
    }

    public UpdateCompanyStatusRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

