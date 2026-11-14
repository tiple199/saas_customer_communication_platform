package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SuperAdminCompanyItemDto {

    private Long id;
    private String name;
    private String logoUrl;
    private String status;
    private LocalDateTime createdAt;

    // Owner info
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;

    // Subscription & Plan info
    private Long planId;
    private String planName;
    private String subscriptionStatus;
    private LocalDateTime subscriptionStartAt;
    private LocalDateTime subscriptionEndAt;

    // Usage counts
    private long memberCount;
    private long channelCount;
    private long conversationCount;

    // Financial
    private BigDecimal totalPaid;

    public SuperAdminCompanyItemDto() {
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public LocalDateTime getSubscriptionStartAt() {
        return subscriptionStartAt;
    }

    public void setSubscriptionStartAt(LocalDateTime subscriptionStartAt) {
        this.subscriptionStartAt = subscriptionStartAt;
    }

    public LocalDateTime getSubscriptionEndAt() {
        return subscriptionEndAt;
    }

    public void setSubscriptionEndAt(LocalDateTime subscriptionEndAt) {
        this.subscriptionEndAt = subscriptionEndAt;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public long getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(long channelCount) {
        this.channelCount = channelCount;
    }

    public long getConversationCount() {
        return conversationCount;
    }

    public void setConversationCount(long conversationCount) {
        this.conversationCount = conversationCount;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }
}

