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
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.ExtendSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminSubscriptionService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminSubscriptionServiceImpl implements SuperAdminSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;

    public SuperAdminSubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                            PlanRepository planRepository,
                                            CompanyMemberRepository companyMemberRepository,
                                            UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
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
    public PageResponse<SuperAdminSubscriptionItemDto> getSubscriptions(
            String currentUserEmail,
            String keyword,
            String status,
            Long planId,
            Boolean isPaid,
            Boolean isExpiringSoon,
            Pageable pageable) {

        validateSuperAdmin(currentUserEmail);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiringThreshold = now.plusDays(7);

        String safeKeyword = (keyword != null && !keyword.trim().isBlank()) ? keyword.trim() : null;
        String safeStatus = (status != null && !status.trim().isBlank() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim().toUpperCase() : null;

        Page<Subscription> page = subscriptionRepository.searchSubscriptions(
                safeKeyword, safeStatus, planId, isPaid, isExpiringSoon, now, expiringThreshold, pageable);

        List<SuperAdminSubscriptionItemDto> dtos = page.getContent().stream()
                .map(this::mapToItemDto)
                .collect(Collectors.toList());

        PageResponse<SuperAdminSubscriptionItemDto> resp = new PageResponse<>();
        resp.setItems(dtos);
        resp.setPage(page.getNumber());
        resp.setSize(page.getSize());
        resp.setTotalElements(page.getTotalElements());
        resp.setTotalPages(page.getTotalPages());
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminSubscriptionStatsDto getSubscriptionStats(String currentUserEmail) {
        validateSuperAdmin(currentUserEmail);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);

        SuperAdminSubscriptionStatsDto stats = new SuperAdminSubscriptionStatsDto();
        stats.setTotalSubscriptions(subscriptionRepository.count());
        stats.setActiveSubscriptions(subscriptionRepository.countByStatus("ACTIVE"));
        stats.setPaidSubscriptions(subscriptionRepository.countActivePaidSubscriptions());
        stats.setFreeSubscriptions(subscriptionRepository.countActiveFreeSubscriptions());
        stats.setExpiringIn7Days(subscriptionRepository.countExpiringSubscriptions(now, in7Days));
        stats.setEstimatedMrr(subscriptionRepository.sumActiveMonthlyRevenue());

        return stats;
    }

    @Override
    @Transactional
    public SuperAdminSubscriptionItemDto extendSubscription(
            String currentUserEmail,
            Long subscriptionId,
            ExtendSubscriptionRequest request) {

        validateSuperAdmin(currentUserEmail);

        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        int months = (request.getMonths() != null && request.getMonths() > 0) ? request.getMonths() : 1;
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime baseDate = (sub.getEndAt() != null && sub.getEndAt().isAfter(now))
                ? sub.getEndAt() : now;

        sub.setEndAt(baseDate.plusMonths(months));
        sub.setStatus("ACTIVE");
        sub = subscriptionRepository.save(sub);

        log.info("[Super Admin] Đã gia hạn thêm {} tháng cho Subscription #{} của công ty ID {}. Ghi chú: {}",
                months, sub.getId(), sub.getCompany() != null ? sub.getCompany().getId() : null, request.getNote());

        return mapToItemDto(sub);
    }

    @Override
    @Transactional
    public SuperAdminSubscriptionItemDto cancelToFree(String currentUserEmail, Long subscriptionId, String reason) {
        validateSuperAdmin(currentUserEmail);

        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        Plan defaultFreePlan = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("Không tìm thấy gói miễn phí mặc định trong hệ thống"));

        sub.setPlan(defaultFreePlan);
        sub.setStartAt(LocalDateTime.now());
        sub.setEndAt(null); // Gói Starter miễn phí vĩnh viễn
        sub.setPendingPlanId(null);
        sub.setStatus("ACTIVE");
        sub = subscriptionRepository.save(sub);

        log.info("[Super Admin] Đã chuyển đổi Subscription #{} về gói miễn phí (Starter). Lý do: {}",
                sub.getId(), reason);

        return mapToItemDto(sub);
    }

    private SuperAdminSubscriptionItemDto mapToItemDto(Subscription sub) {
        SuperAdminSubscriptionItemDto dto = new SuperAdminSubscriptionItemDto();
        dto.setId(sub.getId());
        dto.setStatus(sub.getStatus());
        dto.setStartAt(sub.getStartAt());
        dto.setEndAt(sub.getEndAt());
        dto.setPendingPlanId(sub.getPendingPlanId());
        dto.setCreatedAt(sub.getCreatedAt());

        LocalDateTime now = LocalDateTime.now();

        if (sub.getCompany() != null) {
            Company comp = sub.getCompany();
            dto.setCompanyId(comp.getId());
            dto.setCompanyName(comp.getName());
            dto.setCompanyStatus(comp.getStatus());

            CompanyMember owner = companyMemberRepository.findByIdCompanyIdAndRole(comp.getId(), CompanyMemberRole.OWNER)
                    .stream().findFirst().orElse(null);
            if (owner != null) {
                User ownerUser = userRepository.findById(owner.getId().getUserId()).orElse(null);
                if (ownerUser != null) {
                    dto.setOwnerName(ownerUser.getFullName());
                    dto.setOwnerEmail(ownerUser.getEmail());
                }
            }
        }

        if (sub.getPlan() != null) {
            Plan plan = sub.getPlan();
            dto.setPlanId(plan.getId());
            dto.setPlanName(plan.getName());
            dto.setPlanPrice(plan.getPrice());

            boolean isZeroPrice = plan.getPrice().compareTo(BigDecimal.ZERO) == 0;
            dto.setLifetime(isZeroPrice || sub.getEndAt() == null);
        } else {
            dto.setLifetime(sub.getEndAt() == null);
        }

        if (sub.getEndAt() != null) {
            long days = Duration.between(now, sub.getEndAt()).toDays();
            dto.setDaysRemaining(days);
        }

        if (sub.getPendingPlanId() != null) {
            planRepository.findById(sub.getPendingPlanId()).ifPresent(p -> {
                dto.setPendingPlanName(p.getName());
            });
        }

        return dto;
    }
}
