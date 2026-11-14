package vn.lnt.saas_customer_communication_platform.feature.superadmin.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.ExtendSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionStatsDto;

public interface SuperAdminSubscriptionService {

    PageResponse<SuperAdminSubscriptionItemDto> getSubscriptions(
            String currentUserEmail,
            String keyword,
            String status,
            Long planId,
            Boolean isPaid,
            Boolean isExpiringSoon,
            Pageable pageable
    );

    SuperAdminSubscriptionStatsDto getSubscriptionStats(String currentUserEmail);

    SuperAdminSubscriptionItemDto extendSubscription(
            String currentUserEmail,
            Long subscriptionId,
            ExtendSubscriptionRequest request
    );

    SuperAdminSubscriptionItemDto cancelToFree(
            String currentUserEmail,
            Long subscriptionId,
            String reason
    );
}

