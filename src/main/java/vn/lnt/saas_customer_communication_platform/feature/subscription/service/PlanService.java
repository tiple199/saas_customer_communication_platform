package vn.lnt.saas_customer_communication_platform.feature.subscription.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CreatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.PlanResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpdatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;

import java.util.List;

public interface PlanService {

    PlanResponse createPlan(String currentUserEmail, CreatePlanRequest request);

    List<PlanResponse> getAllPlans(String currentUserEmail, PlanStatus status);

    PageResponse<PlanResponse> getPlansPaged(String currentUserEmail, PlanStatus status, Pageable pageable);

    List<PlanResponse> getActivePlans();

    PlanResponse getPlanById(String currentUserEmail, Long id);

    PlanResponse updatePlan(String currentUserEmail, Long id, UpdatePlanRequest request);

    PlanResponse updatePlanStatus(String currentUserEmail, Long id, PlanStatus status);

    void deletePlan(String currentUserEmail, Long id);

    vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CompanySubscriptionResponse getCompanyCurrentSubscription(String currentUserEmail, Long companyId);

    vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeCalculationResponse calculateUpgradeAmount(String currentUserEmail, Long companyId, Long targetPlanId, Integer durationMonths);

    vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionResponse upgradeCompanySubscription(String currentUserEmail, Long companyId, vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionRequest request);

    vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionResponse requestDowngradeSubscription(String currentUserEmail, Long companyId, vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionRequest request);

    vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CompanySubscriptionResponse cancelPendingDowngrade(String currentUserEmail, Long companyId);
}

