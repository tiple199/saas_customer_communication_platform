package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SuperAdminDashboardResponse {

    // 1. Overview KPIs
    private long totalCompanies;
    private long newCompaniesThisMonth;
    private long totalUsers;
    private long activeSubscriptions;
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private long totalConversations;
    private long totalChannels;

    // 2. Phân bố gói cước
    private List<PlanDistributionDto> planDistributions = new ArrayList<>();

    // 3. Xu hướng doanh thu & tăng trưởng 6 tháng gần nhất
    private List<MonthlyTrendDto> monthlyTrends = new ArrayList<>();

    // 4. Giao dịch thanh toán gần đây (Top 10)
    private List<RecentPaymentDto> recentPayments = new ArrayList<>();

    // 5. Công ty mới tham gia gần đây (Top 10)
    private List<RecentCompanyDto> recentCompanies = new ArrayList<>();

    public SuperAdminDashboardResponse() {
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public long getNewCompaniesThisMonth() {
        return newCompaniesThisMonth;
    }

    public void setNewCompaniesThisMonth(long newCompaniesThisMonth) {
        this.newCompaniesThisMonth = newCompaniesThisMonth;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public void setActiveSubscriptions(long activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
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

    public long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public long getTotalChannels() {
        return totalChannels;
    }

    public void setTotalChannels(long totalChannels) {
        this.totalChannels = totalChannels;
    }

    public List<PlanDistributionDto> getPlanDistributions() {
        return planDistributions;
    }

    public void setPlanDistributions(List<PlanDistributionDto> planDistributions) {
        this.planDistributions = planDistributions;
    }

    public List<MonthlyTrendDto> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<MonthlyTrendDto> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }

    public List<RecentPaymentDto> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<RecentPaymentDto> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public List<RecentCompanyDto> getRecentCompanies() {
        return recentCompanies;
    }

    public void setRecentCompanies(List<RecentCompanyDto> recentCompanies) {
        this.recentCompanies = recentCompanies;
    }
}

