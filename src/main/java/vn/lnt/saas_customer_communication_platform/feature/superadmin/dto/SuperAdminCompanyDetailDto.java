package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyMemberResponse;

import java.util.ArrayList;
import java.util.List;

public class SuperAdminCompanyDetailDto extends SuperAdminCompanyItemDto {

    // Plan Limits & Quota
    private Integer maxUsers;
    private Integer maxChannels;
    private Long storageLimitMb;
    private long usedStorageBytes;
    private boolean aiEnabled;
    private Integer aiQuota;
    private long usedAiQuotaThisMonth;

    // Recent Payments
    private List<RecentPaymentDto> recentPayments = new ArrayList<>();

    // Recent Members
    private List<CompanyMemberResponse> members = new ArrayList<>();

    public SuperAdminCompanyDetailDto() {
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(Integer maxChannels) {
        this.maxChannels = maxChannels;
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

    public List<RecentPaymentDto> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<RecentPaymentDto> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public List<CompanyMemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<CompanyMemberResponse> members) {
        this.members = members;
    }
}
