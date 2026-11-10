package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

public class UsageStatsDto {

    private Integer maxUsers;
    private long activeUsers;
    private Integer maxChannels;
    private long activeChannels;
    private Long storageLimitMb;
    private long usedStorageBytes;
    private boolean aiEnabled;
    private Integer aiQuota;
    private long usedAiQuotaThisMonth;

    public UsageStatsDto() {
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Integer getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(Integer maxChannels) {
        this.maxChannels = maxChannels;
    }

    public long getActiveChannels() {
        return activeChannels;
    }

    public void setActiveChannels(long activeChannels) {
        this.activeChannels = activeChannels;
    }

    public Long getStorageLimitMb() {
        return storageLimitMb;
    }

    public void setStorageLimitMb(Long storageLimitMb) {
        this.storageLimitMb = storageLimitMb;
    }

    public long getUsedStorageBytes() {
        return usedStorageBytes;
    }

    public void setUsedStorageBytes(long usedStorageBytes) {
        this.usedStorageBytes = usedStorageBytes;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public Integer getAiQuota() {
        return aiQuota;
    }

    public void setAiQuota(Integer aiQuota) {
        this.aiQuota = aiQuota;
    }

    public long getUsedAiQuotaThisMonth() {
        return usedAiQuotaThisMonth;
    }

    public void setUsedAiQuotaThisMonth(long usedAiQuotaThisMonth) {
        this.usedAiQuotaThisMonth = usedAiQuotaThisMonth;
    }
}

