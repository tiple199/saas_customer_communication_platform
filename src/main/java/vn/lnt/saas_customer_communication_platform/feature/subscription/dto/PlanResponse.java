package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

import java.math.BigDecimal;

public class PlanResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer maxUser;
    private Integer maxChannel;
    private Boolean aiEnabled;
    private Integer aiQuota;
    private Long storageLimit;
    private PlanStatus status;

    public PlanResponse() {
    }

    public PlanResponse(Long id, String name, BigDecimal price, Integer maxUser, Integer maxChannel,
                        Boolean aiEnabled, Integer aiQuota, Long storageLimit, PlanStatus status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.maxUser = maxUser;
        this.maxChannel = maxChannel;
        this.aiEnabled = aiEnabled;
        this.aiQuota = aiQuota;
        this.storageLimit = storageLimit;
        this.status = status;
    }

    public static PlanResponse fromEntity(Plan plan) {
        if (plan == null) {
            return null;
        }
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getPrice(),
                plan.getMaxUser(),
                plan.getMaxChannel(),
                plan.getAiEnabled(),
                plan.getAiQuota(),
                plan.getStorageLimit(),
                plan.getStatus()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

