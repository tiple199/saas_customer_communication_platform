package vn.lnt.saas_customer_communication_platform.feature.subscription.dto;

import java.math.BigDecimal;

public class UpgradeCalculationResponse {

    private Long targetPlanId;
    private String targetPlanName;
    private BigDecimal targetPlanPrice;
    private Integer durationMonths;
    private BigDecimal fullNewPlanAmount;
    private BigDecimal unusedCreditAmount;
    private Long remainingDays;
    private BigDecimal finalAmountToPay;
    private boolean annualOnly;
    private String annualOnlyReason;

    public UpgradeCalculationResponse() {
    }

    public Long getTargetPlanId() {
        return targetPlanId;
    }

    public void setTargetPlanId(Long targetPlanId) {
        this.targetPlanId = targetPlanId;
    }

    public String getTargetPlanName() {
        return targetPlanName;
    }

    public void setTargetPlanName(String targetPlanName) {
        this.targetPlanName = targetPlanName;
    }

    public BigDecimal getTargetPlanPrice() {
        return targetPlanPrice;
    }

    public void setTargetPlanPrice(BigDecimal targetPlanPrice) {
        this.targetPlanPrice = targetPlanPrice;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public BigDecimal getFullNewPlanAmount() {
        return fullNewPlanAmount;
    }

    public void setFullNewPlanAmount(BigDecimal fullNewPlanAmount) {
        this.fullNewPlanAmount = fullNewPlanAmount;
    }

    public BigDecimal getUnusedCreditAmount() {
        return unusedCreditAmount;
    }

    public void setUnusedCreditAmount(BigDecimal unusedCreditAmount) {
        this.unusedCreditAmount = unusedCreditAmount;
    }

    public Long getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(Long remainingDays) {
        this.remainingDays = remainingDays;
    }

    public BigDecimal getFinalAmountToPay() {
        return finalAmountToPay;
    }

    public void setFinalAmountToPay(BigDecimal finalAmountToPay) {
        this.finalAmountToPay = finalAmountToPay;
    }

    public boolean isAnnualOnly() {
        return annualOnly;
    }

    public void setAnnualOnly(boolean annualOnly) {
        this.annualOnly = annualOnly;
    }

    public String getAnnualOnlyReason() {
        return annualOnlyReason;
    }

    public void setAnnualOnlyReason(String annualOnlyReason) {
        this.annualOnlyReason = annualOnlyReason;
    }
}

