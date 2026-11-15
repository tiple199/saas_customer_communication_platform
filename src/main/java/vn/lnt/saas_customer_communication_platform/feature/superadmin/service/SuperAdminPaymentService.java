package vn.lnt.saas_customer_communication_platform.feature.superadmin.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentStatsDto;

import java.time.LocalDateTime;

public interface SuperAdminPaymentService {

    PageResponse<SuperAdminPaymentItemDto> getPayments(
            String currentUserEmail,
            String keyword,
            String status,
            String provider,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    SuperAdminPaymentStatsDto getPaymentStats(String currentUserEmail);

    SuperAdminPaymentItemDto confirmManualPayment(String currentUserEmail, Long paymentId);

    SuperAdminPaymentItemDto cancelManualPayment(String currentUserEmail, Long paymentId);
}

