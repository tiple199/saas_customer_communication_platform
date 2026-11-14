package vn.lnt.saas_customer_communication_platform.feature.superadmin.dto;

import java.math.BigDecimal;

public class PlanDistributionDto {

    private Long planId;
    private String planName;
    private BigDecimal monthlyPrice;
    private long companyCount;
    private double percentage;

    public PlanDistributionDto() {
    }

    public PlanDistributionDto(Long planId, String planName, BigDecimal monthlyPrice, long companyCount, double percentage) {
        this.planId = planId;
        this.planName = planName;
        this.monthlyPrice = monthlyPrice;
        this.companyCount = companyCount;
        this.percentage = percentage;
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

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public long getCompanyCount() {
        return companyCount;
    }

    public void setCompanyCount(long companyCount) {
        this.companyCount = companyCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

