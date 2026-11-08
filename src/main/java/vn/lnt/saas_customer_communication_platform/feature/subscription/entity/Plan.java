package vn.lnt.saas_customer_communication_platform.feature.subscription.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "max_user")
    private Integer maxUser;

    @Column(name = "max_channel")
    private Integer maxChannel;

    @Column(name = "ai_enabled")
    private Boolean aiEnabled;

    @Column(name = "ai_quota")
    private Integer aiQuota;

    @Column(name = "storage_limit")
    private Long storageLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;

    public Plan() {
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
