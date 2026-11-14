package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;

public class SuperAdminSubscriptionStatsDto {

    private long totalSubscriptions;
    private long activeSubscriptions;
    private long paidSubscriptions;
    private long freeSubscriptions;
    private long expiringIn7Days;
    private BigDecimal estimatedMrr;

    public SuperAdminSubscriptionStatsDto() {
    }

    public long getTotalSubscriptions() {
        return totalSubscriptions;
    }

    public void setTotalSubscriptions(long totalSubscriptions) {
        this.totalSubscriptions = totalSubscriptions;
    }

    public long getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public void setActiveSubscriptions(long activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
    }

    public long getPaidSubscriptions() {
        return paidSubscriptions;
    }

    public void setPaidSubscriptions(long paidSubscriptions) {
        this.paidSubscriptions = paidSubscriptions;
    }

    public long getFreeSubscriptions() {
        return freeSubscriptions;
    }

    public void setFreeSubscriptions(long freeSubscriptions) {
        this.freeSubscriptions = freeSubscriptions;
    }

    public long getExpiringIn7Days() {
        return expiringIn7Days;
    }

    public void setExpiringIn7Days(long expiringIn7Days) {
        this.expiringIn7Days = expiringIn7Days;
    }

    public BigDecimal getEstimatedMrr() {
        return estimatedMrr;
    }

    public void setEstimatedMrr(BigDecimal estimatedMrr) {
        this.estimatedMrr = estimatedMrr;
    }
}

