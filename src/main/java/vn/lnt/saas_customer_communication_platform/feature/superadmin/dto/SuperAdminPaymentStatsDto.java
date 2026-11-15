package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;

public class SuperAdminPaymentStatsDto {

    private long totalPayments;
    private long successCount;
    private long pendingCount;
    private long failedOrCancelledCount;
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;

    public SuperAdminPaymentStatsDto() {
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getFailedOrCancelledCount() {
        return failedOrCancelledCount;
    }

    public void setFailedOrCancelledCount(long failedOrCancelledCount) {
        this.failedOrCancelledCount = failedOrCancelledCount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getRevenueThisMonth() {
        return revenueThisMonth;
    }

    public void setRevenueThisMonth(BigDecimal revenueThisMonth) {
        this.revenueThisMonth = revenueThisMonth;
    }
}

