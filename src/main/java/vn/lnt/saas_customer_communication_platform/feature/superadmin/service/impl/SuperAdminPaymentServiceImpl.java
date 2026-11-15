package vn.lnt.saas_customer_communication_platform.feature.superadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.PaymentWebhookRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PaymentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PaymentService;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminPaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminPaymentServiceImpl implements SuperAdminPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminPaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;

    public SuperAdminPaymentServiceImpl(PaymentRepository paymentRepository,
                                        PaymentService paymentService,
                                        CompanyMemberRepository companyMemberRepository,
                                        UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
    }

    private void validateSuperAdmin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (user.getRole() == null ||
                (!"ROLE_SUPER_ADMIN".equalsIgnoreCase(user.getRole().trim())
                        && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole().trim()))) {
            throw new InvalidOperationException("Chỉ Super Admin mới có quyền truy cập chức năng này");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SuperAdminPaymentItemDto> getPayments(
            String currentUserEmail,
            String keyword,
            String status,
            String provider,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        validateSuperAdmin(currentUserEmail);

        String safeKeyword = (keyword != null && !keyword.trim().isBlank()) ? keyword.trim() : null;
        String safeStatus = (status != null && !status.trim().isBlank() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim().toUpperCase() : null;
        String safeProvider = (provider != null && !provider.trim().isBlank() && !"ALL".equalsIgnoreCase(provider.trim()))
                ? provider.trim().toUpperCase() : null;

        Page<Payment> page = paymentRepository.searchPayments(
                safeKeyword, safeStatus, safeProvider, fromDate, toDate, pageable);

        List<SuperAdminPaymentItemDto> items = page.getContent().stream()
                .map(this::mapToItemDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminPaymentStatsDto getPaymentStats(String currentUserEmail) {
        validateSuperAdmin(currentUserEmail);

        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        SuperAdminPaymentStatsDto stats = new SuperAdminPaymentStatsDto();
        stats.setTotalPayments(paymentRepository.count());
        stats.setSuccessCount(paymentRepository.countByStatus("SUCCESS"));
        stats.setPendingCount(paymentRepository.countByStatus("PENDING"));
        stats.setFailedOrCancelledCount(paymentRepository.countByStatusIn(List.of("CANCELLED", "FAILED")));

        BigDecimal totalRevenue = paymentRepository.calculateTotalRevenue();
        BigDecimal revenueThisMonth = paymentRepository.calculateRevenueSince(startOfMonth);
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.setRevenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO);

        return stats;
    }

    @Override
    @Transactional
    public SuperAdminPaymentItemDto confirmManualPayment(String currentUserEmail, Long paymentId) {
        validateSuperAdmin(currentUserEmail);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidOperationException("Giao dịch này đã được xác nhận thành công trước đó");
        }

        PaymentWebhookRequest webhookReq = new PaymentWebhookRequest();
        webhookReq.setTransactionRef(payment.getTransactionRef());
        webhookReq.setStatus("SUCCESS");
        webhookReq.setProvider(payment.getProvider());

        paymentService.handlePaymentWebhook(webhookReq);

        Payment updated = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        log.info("[Super Admin] {} đã xác nhận thủ công giao dịch #{} ({})",
                currentUserEmail, updated.getId(), updated.getTransactionRef());

        return mapToItemDto(updated);
    }

    @Override
    @Transactional
    public SuperAdminPaymentItemDto cancelManualPayment(String currentUserEmail, Long paymentId) {
        validateSuperAdmin(currentUserEmail);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new InvalidOperationException("Không thể hủy giao dịch đã thanh toán thành công");
        }

        payment.setStatus("CANCELLED");
        payment = paymentRepository.save(payment);

        log.info("[Super Admin] {} đã hủy giao dịch #{} ({})",
                currentUserEmail, payment.getId(), payment.getTransactionRef());

        return mapToItemDto(payment);
    }

    private SuperAdminPaymentItemDto mapToItemDto(Payment payment) {
        SuperAdminPaymentItemDto dto = new SuperAdminPaymentItemDto();
        dto.setId(payment.getId());
        dto.setTransactionRef(payment.getTransactionRef());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setProvider(payment.getProvider());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setPaidAt(payment.getPaidAt());

        String ref = payment.getTransactionRef() != null ? payment.getTransactionRef() : "";
        if (ref.startsWith("RNW")) {
            dto.setOrderType("RENEW");
        } else if (ref.startsWith("UPG")) {
            dto.setOrderType("UPGRADE");
        } else {
            dto.setOrderType("OTHER");
        }

        Subscription sub = payment.getSubscription();
        if (sub != null) {
            dto.setSubscriptionId(sub.getId());
            Plan plan = sub.getPlan();
            if (plan != null) {
                dto.setPlanId(plan.getId());
                dto.setPlanName(plan.getName());
            }

            Company comp = sub.getCompany();
            if (comp != null) {
                dto.setCompanyId(comp.getId());
                dto.setCompanyName(comp.getName());

                CompanyMember owner = companyMemberRepository
                        .findByIdCompanyIdAndRole(comp.getId(), CompanyMemberRole.OWNER)
                        .stream().findFirst().orElse(null);
                if (owner != null) {
                    User ownerUser = userRepository.findById(owner.getId().getUserId()).orElse(null);
                    if (ownerUser != null) {
                        dto.setOwnerName(ownerUser.getFullName());
                        dto.setOwnerEmail(ownerUser.getEmail());
                    }
                }
            }
        }

        return dto;
    }
}

