package vn.lnt.saas_customer_communication_platform.feature.subscription.service;

import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UsageStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;

public interface PlanLimitEnforcementService {

    Plan getEffectivePlan(Long companyId);

    void validateCanCreateChannel(Long companyId, Boolean requestAiEnabled);

    void validateCanActivateChannel(Long companyId);

    void validateCanEnableChannelAi(Long companyId);

    void validateCanAddMember(Long companyId);

    void validateCanUploadDocument(Long companyId, long newFileSizeBytes);

    boolean isAiQuotaAvailable(Long companyId);

    UsageStatsDto getCompanyUsageStats(Long companyId);
}

