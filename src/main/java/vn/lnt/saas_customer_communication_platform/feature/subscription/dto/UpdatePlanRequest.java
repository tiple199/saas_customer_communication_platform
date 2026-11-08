package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

import java.math.BigDecimal;

public class UpdatePlanRequest {

    @Size(min = 1, max = 100, message = "Tên gói dịch vụ phải từ 1 đến 100 ký tự")
    private String name;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói dịch vụ không được âm")
    private BigDecimal price;

    @Min(value = 1, message = "Số lượng người dùng tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxUser;

    @Min(value = 1, message = "Số lượng kênh tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxChannel;

    private Boolean aiEnabled;

    @Min(value = 0, message = "Hạn mức AI không được âm")
    private Integer aiQuota;

    @Min(value = 0, message = "Giới hạn dung lượng lưu trữ không được âm")
    private Long storageLimit;

    private PlanStatus status;

    public UpdatePlanRequest() {
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

