package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import jakarta.validation.constraints.NotNull;

public class SuperAdminAssignPlanRequest {

    @NotNull(message = "Vui lòng chọn gói cước muốn gán")
    private Long planId;

    private Integer durationMonths = 1;

    private String note;

    public SuperAdminAssignPlanRequest() {
    }

    public SuperAdminAssignPlanRequest(Long planId, Integer durationMonths, String note) {
        this.planId = planId;
        this.durationMonths = durationMonths;
        this.note = note;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

