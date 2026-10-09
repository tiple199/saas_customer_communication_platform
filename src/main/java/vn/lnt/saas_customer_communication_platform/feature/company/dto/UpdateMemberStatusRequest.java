package vn.lnt.saas_customer_communication_platform.feature.company.dto;

import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;

public class UpdateMemberStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private CompanyMemberStatus status;

    public UpdateMemberStatusRequest() {
    }

    public UpdateMemberStatusRequest(CompanyMemberStatus status) {
        this.status = status;
    }

    public CompanyMemberStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyMemberStatus status) {
        this.status = status;
    }
}
