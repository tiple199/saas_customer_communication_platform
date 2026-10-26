package vn.lnt.saas_customer_communication_platform.feature.channel.dto;

import vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel;

import java.time.LocalDateTime;

public class ChannelResponse {

    private Long id;
    private String name;
    private String type;
    private String status;
    private String config;
    private LocalDateTime firstConnectedAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long companyId;

    public ChannelResponse() {
    }

    public ChannelResponse(Long id, String name, String type, String status, String config,
                           LocalDateTime firstConnectedAt, LocalDateTime lastSeenAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt, Long companyId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.config = config;
        this.firstConnectedAt = firstConnectedAt;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.companyId = companyId;
    }

    public static ChannelResponse fromEntity(Channel channel) {
        if (channel == null) {
            return null;
        }
        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getType(),
                channel.getStatus(),
                channel.getConfig(),
                channel.getFirstConnectedAt(),
                channel.getLastSeenAt(),
                channel.getCreatedAt(),
                channel.getUpdatedAt(),
                channel.getCompany() != null ? channel.getCompany().getId() : null
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public LocalDateTime getFirstConnectedAt() {
        return firstConnectedAt;
    }

    public void setFirstConnectedAt(LocalDateTime firstConnectedAt) {
        this.firstConnectedAt = firstConnectedAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
