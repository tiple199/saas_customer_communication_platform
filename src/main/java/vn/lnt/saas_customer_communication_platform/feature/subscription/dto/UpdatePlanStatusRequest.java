package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.NotNull;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

public class UpdatePlanStatusRequest {

    @NotNull(message = "Trạng thái gói dịch vụ không được để trống")
    private PlanStatus status;

    public UpdatePlanStatusRequest() {
    }

    public UpdatePlanStatusRequest(PlanStatus status) {
        this.status = status;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }
}

