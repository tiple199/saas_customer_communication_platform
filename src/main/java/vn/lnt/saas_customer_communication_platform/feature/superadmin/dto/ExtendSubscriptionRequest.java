package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ExtendSubscriptionRequest {

    @NotNull(message = "Số tháng gia hạn không được để trống")
    @Min(value = 1, message = "Số tháng gia hạn tối thiểu là 1")
    private Integer months;

    private String note;

    public ExtendSubscriptionRequest() {
    }

    public Integer getMonths() {
        return months;
    }

    public void setMonths(Integer months) {
        this.months = months;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

