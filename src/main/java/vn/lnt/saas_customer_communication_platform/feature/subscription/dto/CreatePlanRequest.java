package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

import java.math.BigDecimal;

public class CreatePlanRequest {

    @NotBlank(message = "Tên gói dịch vụ không được để trống")
    @Size(max = 100, message = "Tên gói dịch vụ không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Giá gói dịch vụ không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói dịch vụ không được âm")
    private BigDecimal price;

    @NotNull(message = "Số lượng người dùng tối đa không được để trống")
    @Min(value = 1, message = "Số lượng người dùng tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxUser;

    @NotNull(message = "Số lượng kênh tối đa không được để trống")
    @Min(value = 1, message = "Số lượng kênh tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxChannel;

    private Boolean aiEnabled = false;

    @Min(value = 0, message = "Hạn mức AI không được âm")
    private Integer aiQuota = 0;

    @NotNull(message = "Giới hạn dung lượng lưu trữ không được để trống")
    @Min(value = 0, message = "Giới hạn dung lượng lưu trữ không được âm")
    private Long storageLimit;

    private PlanStatus status = PlanStatus.ACTIVE;

    public CreatePlanRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getMaxUser() {
        return maxUser;
    }

    public void setMaxUser(Integer maxUser) {
        this.maxUser = maxUser;
    }

    public Integer getMaxChannel() {
        return maxChannel;
    }

    public void setMaxChannel(Integer maxChannel) {
        this.maxChannel = maxChannel;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public Integer getAiQuota() {
        return aiQuota;
    }

    public void setAiQuota(Integer aiQuota) {
        this.aiQuota = aiQuota;
    }

    public Long getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(Long storageLimit) {
        this.storageLimit = storageLimit;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }
}

