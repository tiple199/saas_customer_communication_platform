package vn.lnt.saas_customer_communication_platform.feature.superadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.dto.CompanyMemberResponse;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMember;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberRole;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.conversation.repository.ConversationRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UsageStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PaymentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanLimitEnforcementService;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminCompanyService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminCompanyServiceImpl implements SuperAdminCompanyService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminCompanyServiceImpl.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final ChannelRepository channelRepository;
    private final ConversationRepository conversationRepository;
    private final PlanLimitEnforcementService planLimitEnforcementService;

    public SuperAdminCompanyServiceImpl(UserRepository userRepository,
                                        CompanyRepository companyRepository,
                                        CompanyMemberRepository companyMemberRepository,
                                        SubscriptionRepository subscriptionRepository,
                                        PlanRepository planRepository,
                                        PaymentRepository paymentRepository,
                                        ChannelRepository channelRepository,
                                        ConversationRepository conversationRepository,
                                        PlanLimitEnforcementService planLimitEnforcementService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentRepository = paymentRepository;
        this.channelRepository = channelRepository;
        this.conversationRepository = conversationRepository;
        this.planLimitEnforcementService = planLimitEnforcementService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SuperAdminCompanyItemDto> getCompanies(String currentUserEmail,
                                                               String keyword,
                                                               String status,
                                                               Long planId,
                                                               Pageable pageable) {
        validateSuperAdmin(currentUserEmail);

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String trimmedStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase() : null;

        Page<Company> companyPage = companyRepository.searchCompanies(trimmedKeyword, trimmedStatus, pageable);

        List<SuperAdminCompanyItemDto> items = new ArrayList<>();
        for (Company company : companyPage.getContent()) {
            SuperAdminCompanyItemDto dto = buildCompanyItemDto(company);

            // Filter by planId if requested
            if (planId != null) {
                if (dto.getPlanId() == null || !dto.getPlanId().equals(planId)) {
                    continue;
                }
            }
            items.add(dto);
        }

        return new PageResponse<>(
                items,
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminCompanyDetailDto getCompanyDetail(String currentUserEmail, Long companyId) {
        validateSuperAdmin(currentUserEmail);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        SuperAdminCompanyItemDto baseItem = buildCompanyItemDto(company);
        SuperAdminCompanyDetailDto detail = new SuperAdminCompanyDetailDto();

        // Copy base fields
        detail.setId(baseItem.getId());
        detail.setName(baseItem.getName());
        detail.setLogoUrl(baseItem.getLogoUrl());
        detail.setStatus(baseItem.getStatus());
        detail.setCreatedAt(baseItem.getCreatedAt());
        detail.setOwnerId(baseItem.getOwnerId());
        detail.setOwnerName(baseItem.getOwnerName());
        detail.setOwnerEmail(baseItem.getOwnerEmail());
        detail.setPlanId(baseItem.getPlanId());
        detail.setPlanName(baseItem.getPlanName());
        detail.setSubscriptionStatus(baseItem.getSubscriptionStatus());
        detail.setSubscriptionStartAt(baseItem.getSubscriptionStartAt());
        detail.setSubscriptionEndAt(baseItem.getSubscriptionEndAt());
        detail.setMemberCount(baseItem.getMemberCount());
        detail.setChannelCount(baseItem.getChannelCount());
        detail.setConversationCount(baseItem.getConversationCount());
        detail.setTotalPaid(baseItem.getTotalPaid());

        // Limits & Usage Stats
        UsageStatsDto stats = planLimitEnforcementService.getCompanyUsageStats(companyId);
        detail.setMaxUsers(stats.getMaxUsers());
        detail.setMaxChannels(stats.getMaxChannels());
        detail.setStorageLimitMb(stats.getStorageLimitMb());
        detail.setUsedStorageBytes(stats.getUsedStorageBytes());
        detail.setAiEnabled(stats.isAiEnabled());
        detail.setAiQuota(stats.getAiQuota());
        detail.setUsedAiQuotaThisMonth(stats.getUsedAiQuotaThisMonth());

        // Recent Payments
        Page<Payment> paymentPage = paymentRepository.findBySubscriptionCompanyIdOrderByCreatedAtDesc(
                companyId, PageRequest.of(0, 10));
        List<RecentPaymentDto> paymentDtos = paymentPage.getContent().stream().map(p -> {
            RecentPaymentDto pDto = new RecentPaymentDto();
            pDto.setId(p.getId());
            pDto.setTransactionRef(p.getTransactionRef());
            pDto.setCompanyId(companyId);
            pDto.setCompanyName(company.getName());
            if (p.getSubscription() != null && p.getSubscription().getPlan() != null) {
                pDto.setPlanName(p.getSubscription().getPlan().getName());
            }
            pDto.setAmount(p.getAmount());
            pDto.setCurrency(p.getCurrency());
            pDto.setProvider(p.getProvider());
            pDto.setStatus(p.getStatus());
            pDto.setCreatedAt(p.getCreatedAt());
            pDto.setPaidAt(p.getPaidAt());
            return pDto;
        }).collect(Collectors.toList());
        detail.setRecentPayments(paymentDtos);

        // Members
        List<CompanyMember> members = companyMemberRepository.findByIdCompanyId(companyId);
        List<CompanyMemberResponse> memberResponses = new ArrayList<>();
        for (CompanyMember cm : members) {
            User u = userRepository.findById(cm.getId().getUserId()).orElse(null);
            memberResponses.add(CompanyMemberResponse.fromEntity(cm, u));
        }
        detail.setMembers(memberResponses);

        return detail;
    }

    @Override
    @Transactional
    public SuperAdminCompanyItemDto updateCompanyStatus(String currentUserEmail,
                                                        Long companyId,
                                                        UpdateCompanyStatusRequest request) {
        validateSuperAdmin(currentUserEmail);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        String newStatus = request.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus) && !"SUSPENDED".equals(newStatus)) {
            throw new InvalidOperationException("Trạng thái công ty không hợp lệ. Chỉ chấp nhận ACTIVE, INACTIVE hoặc SUSPENDED");
        }

        company.setStatus(newStatus);
        company = companyRepository.save(company);
        log.info("[Super Admin] Đã cập nhật trạng thái công ty #{} ({}) sang {}. Lý do: {}",
                company.getId(), company.getName(), newStatus, request.getReason());

        return buildCompanyItemDto(company);
    }

    @Override
    @Transactional
    public SuperAdminCompanyDetailDto assignPlan(String currentUserEmail,
                                                 Long companyId,
                                                 SuperAdminAssignPlanRequest request) {
        validateSuperAdmin(currentUserEmail);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getPlanId()));

        int durationMonths = (request.getDurationMonths() != null && request.getDurationMonths() > 0)
                ? request.getDurationMonths() : 1;

        LocalDateTime now = LocalDateTime.now();

        Subscription sub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(companyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId))
                .orElse(null);

        boolean isFreePlan = plan.getPrice() != null && plan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0;

        if (sub == null) {
            sub = new Subscription();
            sub.setCompany(company);
            sub.setPlan(plan);
            sub.setStatus("ACTIVE");
            sub.setStartAt(now);
            sub.setEndAt(isFreePlan ? null : now.plusMonths(durationMonths));
        } else {
            sub.setPlan(plan);
            sub.setStatus("ACTIVE");
            if (isFreePlan) {
                sub.setEndAt(null);
            } else {
                LocalDateTime effectiveStart = (sub.getEndAt() != null && sub.getEndAt().isAfter(now))
                        ? sub.getEndAt() : now;
                sub.setEndAt(effectiveStart.plusMonths(durationMonths));
            }
            sub.setPendingPlanId(null);
        }

        subscriptionRepository.save(sub);
        log.info("[Super Admin] Đã gán gói {} cho công ty #{} ({}) với thời hạn {} tháng. Ghi chú: {}",
                plan.getName(), company.getId(), company.getName(), durationMonths, request.getNote());

        return getCompanyDetail(currentUserEmail, companyId);
    }

    private SuperAdminCompanyItemDto buildCompanyItemDto(Company company) {
        SuperAdminCompanyItemDto dto = new SuperAdminCompanyItemDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setStatus(company.getStatus());
        dto.setCreatedAt(company.getCreatedAt());

        // Find Owner
        CompanyMember ownerMember = companyMemberRepository.findByIdCompanyIdAndRole(company.getId(), CompanyMemberRole.OWNER)
                .orElse(null);
        if (ownerMember != null) {
            User ownerUser = userRepository.findById(ownerMember.getId().getUserId()).orElse(null);
            if (ownerUser != null) {
                dto.setOwnerId(ownerUser.getId());
                dto.setOwnerName(ownerUser.getFullName());
                dto.setOwnerEmail(ownerUser.getEmail());
            }
        }

        // Find Subscription
        Subscription sub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(company.getId(), "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(company.getId()))
                .orElse(null);

        if (sub != null && sub.getPlan() != null) {
            dto.setPlanId(sub.getPlan().getId());
            dto.setPlanName(sub.getPlan().getName());
            dto.setSubscriptionStatus(sub.getStatus());
            dto.setSubscriptionStartAt(sub.getStartAt());
            dto.setSubscriptionEndAt(sub.getEndAt());
        } else {
            dto.setPlanName("Chưa kích hoạt");
            dto.setSubscriptionStatus("NONE");
        }

        // Counts
        long members = companyMemberRepository.countByIdCompanyIdAndStatus(company.getId(), CompanyMemberStatus.ACTIVE);
        long channels = channelRepository.countByCompanyIdAndStatus(company.getId(), ChannelStatus.ACTIVE);
        long conversations = conversationRepository.countByChannelCompanyId(company.getId());
        BigDecimal totalPaid = paymentRepository.sumPaidAmountByCompanyId(company.getId());

        dto.setMemberCount(members);
        dto.setChannelCount(channels);
        dto.setConversationCount(conversations);
        dto.setTotalPaid(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        return dto;
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
}

