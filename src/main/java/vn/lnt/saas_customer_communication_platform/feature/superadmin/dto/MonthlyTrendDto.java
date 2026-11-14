package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;

public class MonthlyTrendDto {

    private String month;
    private String label;
    private BigDecimal revenue;
    private long transactionCount;
    private long newCompanies;

    public MonthlyTrendDto() {
    }

    public MonthlyTrendDto(String month, String label, BigDecimal revenue, long transactionCount, long newCompanies) {
        this.month = month;
        this.label = label;
        this.revenue = revenue;
        this.transactionCount = transactionCount;
        this.newCompanies = newCompanies;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getNewCompanies() {
        return newCompanies;
    }

    public void setNewCompanies(long newCompanies) {
        this.newCompanies = newCompanies;
    }
}

