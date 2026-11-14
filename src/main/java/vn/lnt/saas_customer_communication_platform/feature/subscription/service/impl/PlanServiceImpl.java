package vn.lnt.saas_customer_communication_platform.feature.subscription.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.exception.DuplicateResourceException;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CreatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CompanySubscriptionResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.PlanResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.RenewSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.RenewSubscriptionResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpdatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeCalculationResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Payment;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Subscription;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PaymentRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.channel.repository.ChannelRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.Company;
import vn.lnt.saas_customer_communication_platform.feature.company.entity.CompanyMemberStatus;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyMemberRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.repository.CompanyRepository;
import vn.lnt.saas_customer_communication_platform.feature.company.security.CompanySecurityEvaluator;
import vn.lnt.saas_customer_communication_platform.config.tenant.TenantContext;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanLimitEnforcementService;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanServiceImpl.class);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final ChannelRepository channelRepository;
    private final CompanySecurityEvaluator companySecurityEvaluator;
    private final PlanLimitEnforcementService planLimitEnforcementService;

    public PlanServiceImpl(PlanRepository planRepository,
                           SubscriptionRepository subscriptionRepository,
                           PaymentRepository paymentRepository,
                           UserRepository userRepository,
                           CompanyRepository companyRepository,
                           CompanyMemberRepository companyMemberRepository,
                           ChannelRepository channelRepository,
                           CompanySecurityEvaluator companySecurityEvaluator,
                           PlanLimitEnforcementService planLimitEnforcementService) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.channelRepository = channelRepository;
        this.companySecurityEvaluator = companySecurityEvaluator;
        this.planLimitEnforcementService = planLimitEnforcementService;
    }

    public static boolean isSuperAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "ROLE_SUPER_ADMIN".equals(normalized)
                || "SUPER_ADMIN".equals(normalized)
                || "ROLE_ADMIN".equals(normalized);
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidOperationException("Bạn chưa đăng nhập");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private User validateSuperAdmin(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        if (!isSuperAdminRole(currentUser.getRole())) {
            throw new InvalidOperationException("Chỉ Super Admin mới có quyền quản lý gói dịch vụ (Plan)");
        }
        return currentUser;
    }

    private Plan getPlanByIdOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    @Override
    @Transactional
    public PlanResponse createPlan(String currentUserEmail, CreatePlanRequest request) {
        validateSuperAdmin(currentUserEmail);

        String trimmedName = request.getName().trim();
        if (planRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Plan", "name", trimmedName);
        }

        boolean aiEnabled = Boolean.TRUE.equals(request.getAiEnabled());
        int aiQuota = aiEnabled
                ? (request.getAiQuota() != null ? request.getAiQuota() : 0)
                : 0;

        Plan plan = new Plan();
        plan.setName(trimmedName);
        plan.setPrice(request.getPrice());
        plan.setMaxUser(request.getMaxUser());
        plan.setMaxChannel(request.getMaxChannel());
        plan.setAiEnabled(aiEnabled);
        plan.setAiQuota(aiQuota);
        plan.setStorageLimit(request.getStorageLimit());
        plan.setStatus(request.getStatus() != null ? request.getStatus() : PlanStatus.ACTIVE);

        Plan savedPlan = planRepository.save(plan);
        return PlanResponse.fromEntity(savedPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans(String currentUserEmail, PlanStatus status) {
        validateSuperAdmin(currentUserEmail);

        List<Plan> plans = status != null
                ? planRepository.findByStatusOrderByPriceAsc(status)
                : planRepository.findAllByOrderByPriceAsc();

        return plans.stream()
                .map(PlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlanResponse> getPlansPaged(String currentUserEmail, PlanStatus status, Pageable pageable) {
        validateSuperAdmin(currentUserEmail);

        int safePage = Math.max(pageable.getPageNumber(), 0);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "price");

        Pageable effectivePageable = PageRequest.of(safePage, safeSize, sort);
        Page<Plan> pageResult = status != null
                ? planRepository.findByStatus(status, effectivePageable)
                : planRepository.findAll(effectivePageable);

        List<PlanResponse> items = pageResult.getContent().stream()
                .map(PlanResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                .map(PlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(String currentUserEmail, Long id) {
        User currentUser = getUserByEmail(currentUserEmail);
        Plan plan = getPlanByIdOrThrow(id);

        if (plan.getStatus() == PlanStatus.INACTIVE && !isSuperAdminRole(currentUser.getRole())) {
            throw new InvalidOperationException("Gói dịch vụ này hiện đã ngừng cung cấp");
        }

        return PlanResponse.fromEntity(plan);
    }

    @Override
    @Transactional
    public PlanResponse updatePlan(String currentUserEmail, Long id, UpdatePlanRequest request) {
        validateSuperAdmin(currentUserEmail);
        Plan plan = getPlanByIdOrThrow(id);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String trimmedName = request.getName().trim();
            if (planRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new DuplicateResourceException("Plan", "name", trimmedName);
            }
            plan.setName(trimmedName);
        }

        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getMaxUser() != null) {
            plan.setMaxUser(request.getMaxUser());
        }
        if (request.getMaxChannel() != null) {
            plan.setMaxChannel(request.getMaxChannel());
        }
        if (request.getAiEnabled() != null) {
            plan.setAiEnabled(request.getAiEnabled());
            if (!request.getAiEnabled() && request.getAiQuota() == null) {
                plan.setAiQuota(0);
            }
        }
        if (request.getAiQuota() != null) {
            plan.setAiQuota(Boolean.TRUE.equals(plan.getAiEnabled()) ? request.getAiQuota() : 0);
        }
        if (request.getStorageLimit() != null) {
            plan.setStorageLimit(request.getStorageLimit());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }

        Plan updatedPlan = planRepository.save(plan);
        return PlanResponse.fromEntity(updatedPlan);
    }

    @Override
    @Transactional
    public PlanResponse updatePlanStatus(String currentUserEmail, Long id, PlanStatus status) {
        validateSuperAdmin(currentUserEmail);
        Plan plan = getPlanByIdOrThrow(id);

        plan.setStatus(status);
        Plan updatedPlan = planRepository.save(plan);
        return PlanResponse.fromEntity(updatedPlan);
    }

    @Override
    @Transactional
    public void deletePlan(String currentUserEmail, Long id) {
        validateSuperAdmin(currentUserEmail);
        Plan plan = getPlanByIdOrThrow(id);

        if (subscriptionRepository.existsByPlanId(id) || subscriptionRepository.existsByPendingPlanId(id)) {
            throw new InvalidOperationException(
                    "Không thể xóa gói dịch vụ đang có công ty đăng ký sử dụng. Vui lòng chuyển trạng thái sang Ngừng hoạt động (INACTIVE)"
            );
        }

        planRepository.delete(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySubscriptionResponse getCompanyCurrentSubscription(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isMember(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Bạn không có quyền truy cập thông tin gói cước của công ty này");
        }

        Subscription sub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElse(null);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // Nếu gói cước trả phí hiện tại đã quá ngày hết hạn (endAt), kích hoạt gói pendingPlan hoặc về gói mặc định Starter (Miễn phí)
        if (sub != null && sub.getPlan() != null && sub.getPlan().getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (sub.getEndAt() != null && sub.getEndAt().isBefore(now)) {
                Plan nextPlan = null;
                if (sub.getPendingPlanId() != null) {
                    nextPlan = planRepository.findById(sub.getPendingPlanId()).orElse(null);
                }
                if (nextPlan == null) {
                    nextPlan = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                            .findFirst()
                            .orElse(null);
                }
                if (nextPlan != null) {
                    sub.setPlan(nextPlan);
                    sub.setStatus("ACTIVE");
                    sub.setStartAt(now);
                    if (nextPlan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                        sub.setEndAt(null); // Gói free duy trì vĩnh viễn (không giới hạn thời gian)
                    } else {
                        sub.setEndAt(now.plusMonths(1)); // Gói trả phí
                    }
                    sub.setPendingPlanId(null);
                    sub = subscriptionRepository.save(sub);
                }
            }
        }

        if (sub == null) {
            // Nếu công ty chưa có subscription nào trong DB, tạo và trả về gói mặc định Starter (miễn phí)
            Plan defaultPlan = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                    .findFirst()
                    .orElse(null);

            CompanySubscriptionResponse mockResp = new CompanySubscriptionResponse();
            mockResp.setCompanyId(targetCompanyId);
            mockResp.setStatus("ACTIVE");
            mockResp.setPlan(PlanResponse.fromEntity(defaultPlan));
            mockResp.setUsage(planLimitEnforcementService.getCompanyUsageStats(targetCompanyId));
            return mockResp;
        }

        PlanResponse pendingPlanResp = null;
        if (sub.getPendingPlanId() != null) {
            pendingPlanResp = planRepository.findById(sub.getPendingPlanId())
                    .map(PlanResponse::fromEntity)
                    .orElse(null);
        }

        CompanySubscriptionResponse resp = CompanySubscriptionResponse.fromEntity(sub, pendingPlanResp);
        if (resp != null) {
            resp.setUsage(planLimitEnforcementService.getCompanyUsageStats(targetCompanyId));
        }
        return resp;
    }

    private boolean isAnnualSubscription(Subscription currentSub, java.time.LocalDateTime now) {
        if (currentSub == null || currentSub.getEndAt() == null || currentSub.getEndAt().isBefore(now)) {
            return false;
        }
        if (currentSub.getPlan() == null || currentSub.getPlan().getPrice() == null
                || currentSub.getPlan().getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        // Chu kỳ ban đầu > 60 ngày hoặc số ngày còn lại > 30 ngày
        if (currentSub.getStartAt() != null && java.time.Duration.between(currentSub.getStartAt(), currentSub.getEndAt()).toDays() > 60) {
            return true;
        }
        return java.time.Duration.between(now, currentSub.getEndAt()).toDays() > 30;
    }

    @Override
    @Transactional(readOnly = true)
    public UpgradeCalculationResponse calculateUpgradeAmount(String currentUserEmail, Long companyId, Long targetPlanId, Integer durationMonths) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Bạn không có quyền xem thông tin nâng cấp của công ty này");
        }

        Plan targetPlan = planRepository.findById(targetPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", targetPlanId));

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElse(null);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        boolean isAnnual = isAnnualSubscription(currentSub, now);

        int months = (durationMonths != null && (durationMonths == 1 || durationMonths == 12)) ? durationMonths : (isAnnual ? 12 : 1);
        if (isAnnual) {
            months = 12; // Khóa chu kỳ 1 Năm nếu gói hiện tại là gói Năm
        }

        java.math.BigDecimal fullNewPlanAmount = targetPlan.getPrice().multiply(java.math.BigDecimal.valueOf(months));
        java.math.BigDecimal unusedCredit = java.math.BigDecimal.ZERO;
        long remainingDays = 0;

        // Tính tiền thừa (Proration) nếu gói hiện tại có phí và còn ngày sử dụng
        if (currentSub != null && currentSub.getPlan() != null
                && currentSub.getPlan().getPrice().compareTo(java.math.BigDecimal.ZERO) > 0
                && currentSub.getEndAt() != null && currentSub.getEndAt().isAfter(now)) {

            java.time.Duration durationRemaining = java.time.Duration.between(now, currentSub.getEndAt());
            remainingDays = Math.max(1, durationRemaining.toDays());

            // Đơn giá mỗi ngày của gói cũ = Giá 1 tháng / 30 ngày
            java.math.BigDecimal dailyRate = currentSub.getPlan().getPrice()
                    .divide(java.math.BigDecimal.valueOf(30), 4, java.math.RoundingMode.HALF_UP);

            unusedCredit = dailyRate.multiply(java.math.BigDecimal.valueOf(remainingDays))
                    .setScale(0, java.math.RoundingMode.DOWN);

            // Không để tiền thừa vượt quá giá trị gói mới
            if (unusedCredit.compareTo(fullNewPlanAmount) > 0) {
                unusedCredit = fullNewPlanAmount;
            }
        }

        java.math.BigDecimal finalAmount = fullNewPlanAmount.subtract(unusedCredit);
        if (finalAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            finalAmount = java.math.BigDecimal.ZERO;
        }

        UpgradeCalculationResponse resp = new UpgradeCalculationResponse();
        resp.setTargetPlanId(targetPlan.getId());
        resp.setTargetPlanName(targetPlan.getName());
        resp.setTargetPlanPrice(targetPlan.getPrice());
        resp.setDurationMonths(months);
        resp.setFullNewPlanAmount(fullNewPlanAmount);
        resp.setUnusedCreditAmount(unusedCredit);
        resp.setRemainingDays(remainingDays);
        resp.setFinalAmountToPay(finalAmount);
        resp.setAnnualOnly(isAnnual);
        if (isAnnual) {
            resp.setAnnualOnlyReason("Gói hiện tại của bạn là gói theo Năm (còn " + remainingDays + " ngày). Khi nâng cấp lên gói cao hơn, chu kỳ thanh toán áp dụng theo Năm (12 Tháng) để bảo toàn giá trị khấu trừ tiền thừa.");
        }
        return resp;
    }

    @Override
    @Transactional
    public UpgradeSubscriptionResponse upgradeCompanySubscription(String currentUserEmail, Long companyId, UpgradeSubscriptionRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        // Chỉ OWNER hoặc ADMIN của công ty (hoặc Super Admin) mới có quyền nâng cấp gói cước
        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Chỉ chủ sở hữu (Owner) hoặc quản trị viên (Admin) của công ty mới có quyền nâng cấp gói dịch vụ");
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", targetCompanyId));

        Plan targetPlan = planRepository.findById(request.getTargetPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getTargetPlanId()));

        if (targetPlan.getStatus() != PlanStatus.ACTIVE) {
            throw new InvalidOperationException("Gói dịch vụ được chọn hiện không còn mở đăng ký mới");
        }

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElse(null);

        Plan previousPlan = currentSub != null ? currentSub.getPlan() : null;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        int inputMonths = request.getDurationMonths() != null ? request.getDurationMonths() : 1;
        if (inputMonths != 1 && inputMonths != 12) {
            throw new InvalidOperationException("Hệ thống hiện tại chỉ hỗ trợ chu kỳ thanh toán 1 tháng hoặc 1 năm (12 tháng)");
        }

        boolean isAnnual = isAnnualSubscription(currentSub, now);
        if (isAnnual && inputMonths != 12) {
            throw new InvalidOperationException("Gói hiện tại của bạn là gói theo Năm. Khi nâng cấp lên gói cao hơn, bạn cần chọn chu kỳ 1 Năm (12 Tháng) để bảo toàn giá trị khấu trừ tiền thừa.");
        }
        int months = isAnnual ? 12 : inputMonths;

        // 1. Tính toán giá gói mới
        java.math.BigDecimal fullNewPlanAmount = targetPlan.getPrice().multiply(java.math.BigDecimal.valueOf(months));
        java.math.BigDecimal unusedCredit = java.math.BigDecimal.ZERO;
        long remainingDays = 0;

        // 2. Tính tiền thừa (Proration) nếu gói cũ có phí và còn ngày sử dụng
        if (currentSub != null && currentSub.getPlan() != null
                && currentSub.getPlan().getPrice().compareTo(java.math.BigDecimal.ZERO) > 0
                && currentSub.getEndAt() != null && currentSub.getEndAt().isAfter(now)) {

            java.time.Duration durationRemaining = java.time.Duration.between(now, currentSub.getEndAt());
            remainingDays = Math.max(1, durationRemaining.toDays());

            java.math.BigDecimal dailyRate = currentSub.getPlan().getPrice()
                    .divide(java.math.BigDecimal.valueOf(30), 4, java.math.RoundingMode.HALF_UP);

            unusedCredit = dailyRate.multiply(java.math.BigDecimal.valueOf(remainingDays))
                    .setScale(0, java.math.RoundingMode.DOWN);

            if (unusedCredit.compareTo(fullNewPlanAmount) > 0) {
                unusedCredit = fullNewPlanAmount;
            }
        }

        // 3. Số tiền thực tế phải thanh toán sau khi khấu trừ tiền thừa
        java.math.BigDecimal amountToPay = fullNewPlanAmount.subtract(unusedCredit);
        if (amountToPay.compareTo(java.math.BigDecimal.ZERO) < 0) {
            amountToPay = java.math.BigDecimal.ZERO;
        }

        // 4. Hạn dùng mới bắt đầu tính từ ngày nâng cấp
        java.time.LocalDateTime endAt = now.plusMonths(months);

        Subscription targetSubscription;
        if (currentSub != null) {
            currentSub.setPlan(targetPlan);
            currentSub.setStatus("ACTIVE");
            currentSub.setStartAt(now);
            currentSub.setEndAt(endAt);
            currentSub.setPendingPlanId(null);
            targetSubscription = subscriptionRepository.save(currentSub);
        } else {
            Subscription newSub = new Subscription();
            newSub.setCompany(company);
            newSub.setPlan(targetPlan);
            newSub.setStatus("ACTIVE");
            newSub.setStartAt(now);
            newSub.setEndAt(endAt);
            targetSubscription = subscriptionRepository.save(newSub);
        }

        String transactionRef = "TXN_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        Payment payment = new Payment();
        payment.setSubscription(targetSubscription);
        payment.setAmount(amountToPay);
        payment.setCurrency("VND");
        payment.setProvider(request.getPaymentProvider() != null ? request.getPaymentProvider() : "SIMULATED");
        payment.setStatus("SUCCESS");
        payment.setTransactionRef(transactionRef);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        UpgradeSubscriptionResponse resp = new UpgradeSubscriptionResponse();
        resp.setSubscriptionId(targetSubscription.getId());
        resp.setCompanyId(targetCompanyId);
        resp.setPreviousPlan(PlanResponse.fromEntity(previousPlan));
        resp.setUpgradedPlan(PlanResponse.fromEntity(targetPlan));
        resp.setStatus("ACTIVE");
        resp.setStartAt(targetSubscription.getStartAt());
        resp.setEndAt(targetSubscription.getEndAt());
        resp.setAmountPaid(amountToPay);
        resp.setFullPrice(fullNewPlanAmount);
        resp.setUnusedCreditAmount(unusedCredit);
        resp.setRemainingDaysCredited(remainingDays);
        resp.setPaymentProvider(payment.getProvider());
        resp.setTransactionRef(transactionRef);

        if (unusedCredit.compareTo(java.math.BigDecimal.ZERO) > 0) {
            resp.setMessage(String.format("Nâng cấp thành công sang %s. Đã khấu trừ %s (tiền thừa %d ngày gói cũ).",
                    targetPlan.getName(),
                    new java.text.DecimalFormat("#,### ₫").format(unusedCredit),
                    remainingDays));
        } else {
            resp.setMessage("Nâng cấp gói dịch vụ thành công sang " + targetPlan.getName());
        }

        return resp;
    }

    @Override
    @Transactional
    public DowngradeSubscriptionResponse requestDowngradeSubscription(String currentUserEmail, Long companyId, DowngradeSubscriptionRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Chỉ chủ sở hữu (Owner) hoặc quản trị viên (Admin) của công ty mới có quyền hạ cấp gói dịch vụ");
        }

        Plan targetPlan = planRepository.findById(request.getTargetPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getTargetPlanId()));

        if (targetPlan.getStatus() != PlanStatus.ACTIVE) {
            throw new InvalidOperationException("Gói dịch vụ muốn hạ cấp hiện không còn hoạt động");
        }

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElse(null);

        Plan currentPlan = currentSub != null ? currentSub.getPlan() : null;

        if (currentPlan != null && currentPlan.getId().equals(targetPlan.getId())) {
            throw new InvalidOperationException("Công ty của bạn hiện đang sử dụng gói này rồi");
        }

        // Kiểm tra nếu giá gói muốn hạ lại lớn hơn gói hiện tại
        if (currentPlan != null && targetPlan.getPrice().compareTo(currentPlan.getPrice()) >= 0) {
            throw new InvalidOperationException("Gói được chọn có mức giá cao hơn hoặc tương đương gói hiện tại. Vui lòng sử dụng tính năng Nâng cấp gói.");
        }

        // Kiểm tra giới hạn tài nguyên trước khi cho phép hạ cấp
        long activeMembersCount = companyMemberRepository.countByIdCompanyIdAndStatus(targetCompanyId, CompanyMemberStatus.ACTIVE);
        if (targetPlan.getMaxUser() != null && activeMembersCount > targetPlan.getMaxUser()) {
            throw new InvalidOperationException(
                    String.format("Không thể hạ cấp: Công ty hiện đang có %d thành viên hoạt động, vượt quá giới hạn tối đa (%d thành viên) của gói %s. Vui lòng giảm bớt thành viên trước.",
                            activeMembersCount, targetPlan.getMaxUser(), targetPlan.getName())
            );
        }

        long channelsCount = channelRepository.countByCompanyId(targetCompanyId);
        if (targetPlan.getMaxChannel() != null && channelsCount > targetPlan.getMaxChannel()) {
            throw new InvalidOperationException(
                    String.format("Không thể hạ cấp: Công ty hiện đang kết nối %d kênh, vượt quá giới hạn tối đa (%d kênh) của gói %s. Vui lòng gỡ bớt kênh trước.",
                            channelsCount, targetPlan.getMaxChannel(), targetPlan.getName())
            );
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime effectiveDate;

        // Nếu công ty chưa có subscription hoặc gói hiện tại là Miễn phí hoặc gói đã hết hạn -> Hạ cấp ngay lập tức
        boolean immediateDowngrade = currentSub == null
                || currentSub.getEndAt() == null
                || currentSub.getEndAt().isBefore(now)
                || (currentPlan != null && currentPlan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0);

        if (immediateDowngrade) {
            if (currentSub == null) {
                Company company = companyRepository.findById(targetCompanyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Company", "id", targetCompanyId));
                currentSub = new Subscription();
                currentSub.setCompany(company);
            }
            currentSub.setPlan(targetPlan);
            currentSub.setStatus("ACTIVE");
            currentSub.setStartAt(now);
            currentSub.setEndAt(targetPlan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0 ? null : now.plusMonths(1)); // Gói free duy trì vĩnh viễn
            currentSub.setPendingPlanId(null);
            subscriptionRepository.save(currentSub);
            effectiveDate = now;
        } else {
            // Đặt lịch hạ cấp vào cuối chu kỳ hiện tại
            currentSub.setPendingPlanId(targetPlan.getId());
            subscriptionRepository.save(currentSub);
            effectiveDate = currentSub.getEndAt();
        }

        DowngradeSubscriptionResponse resp = new DowngradeSubscriptionResponse();
        resp.setSubscriptionId(currentSub.getId());
        resp.setCompanyId(targetCompanyId);
        resp.setCurrentPlan(PlanResponse.fromEntity(currentPlan));
        resp.setPendingPlan(PlanResponse.fromEntity(targetPlan));
        resp.setEffectiveDate(effectiveDate);
        resp.setMessage(immediateDowngrade
                ? "Đã hạ cấp sang gói " + targetPlan.getName() + " thành công"
                : "Đã ghi nhận yêu cầu hạ cấp sang gói " + targetPlan.getName() + ". Gói mới sẽ chính thức có hiệu lực vào cuối chu kỳ hiện tại (" + effectiveDate.toLocalDate() + ").");

        return resp;
    }

    @Override
    @Transactional
    public CompanySubscriptionResponse cancelPendingDowngrade(String currentUserEmail, Long companyId) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Chỉ chủ sở hữu (Owner) hoặc quản trị viên (Admin) của công ty mới có quyền hủy yêu cầu hạ cấp");
        }

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElseThrow(() -> new InvalidOperationException("Không tìm thấy thông tin gói dịch vụ của công ty"));

        if (currentSub.getPendingPlanId() == null) {
            throw new InvalidOperationException("Công ty không có yêu cầu hạ cấp gói nào đang chờ xử lý");
        }

        currentSub.setPendingPlanId(null);
        Subscription saved = subscriptionRepository.save(currentSub);

        return CompanySubscriptionResponse.fromEntity(saved, null);
    }

    @Override
    @Transactional
    public RenewSubscriptionResponse renewCompanySubscription(String currentUserEmail, Long companyId, RenewSubscriptionRequest request) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long targetCompanyId = companyId != null ? companyId : TenantContext.getTenantId();
        if (targetCompanyId == null) {
            throw new InvalidOperationException("Bạn chưa chọn công ty hoạt động");
        }

        if (!isSuperAdminRole(currentUser.getRole()) && !companySecurityEvaluator.isOwnerOrAdmin(currentUser.getId(), targetCompanyId)) {
            throw new InvalidOperationException("Chỉ chủ sở hữu (Owner) hoặc quản trị viên (Admin) của công ty mới có quyền gia hạn gói dịch vụ");
        }

        Subscription currentSub = subscriptionRepository.findFirstByCompanyIdAndStatusOrderByEndAtDesc(targetCompanyId, "ACTIVE")
                .or(() -> subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(targetCompanyId))
                .orElseThrow(() -> new InvalidOperationException("Công ty chưa đăng ký gói dịch vụ nào"));

        Plan currentPlan = currentSub.getPlan();
        if (currentPlan == null || currentPlan.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Gói dịch vụ hiện tại là gói Miễn phí (Starter), không cần gia hạn");
        }

        int inputMonths = request.getDurationMonths() != null ? request.getDurationMonths() : 1;
        if (inputMonths != 1 && inputMonths != 12) {
            throw new InvalidOperationException("Hệ thống chỉ hỗ trợ gia hạn chu kỳ 1 tháng hoặc 1 năm (12 tháng)");
        }
        int months = inputMonths;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime previousEndAt = currentSub.getEndAt();

        // NGUYÊN TẮC CỘNG DỒN NGÀY (DATE STACKING):
        // Nếu gói cũ vẫn còn hạn, cộng dồn tiếp từ ngày hết hạn hiện tại.
        // Nếu đã quá hạn, tính mới từ thời điểm thanh toán (now).
        java.time.LocalDateTime baseDate = (previousEndAt != null && previousEndAt.isAfter(now)) ? previousEndAt : now;
        java.time.LocalDateTime newEndAt = baseDate.plusMonths(months);

        java.math.BigDecimal amountToPay = currentPlan.getPrice().multiply(java.math.BigDecimal.valueOf(months));

        currentSub.setStatus("ACTIVE");
        currentSub.setEndAt(newEndAt);
        // Nếu trước đó có đặt lịch hạ cấp (pendingPlanId), tự động hủy lịch hạ cấp vì công ty đã chọn gia hạn gói hiện tại
        currentSub.setPendingPlanId(null);
        Subscription savedSub = subscriptionRepository.save(currentSub);

        String transactionRef = "RENEW_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        Payment payment = new Payment();
        payment.setSubscription(savedSub);
        payment.setAmount(amountToPay);
        payment.setCurrency("VND");
        payment.setProvider(request.getPaymentProvider() != null ? request.getPaymentProvider() : "SIMULATED");
        payment.setStatus("SUCCESS");
        payment.setTransactionRef(transactionRef);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        RenewSubscriptionResponse resp = new RenewSubscriptionResponse();
        resp.setSubscriptionId(savedSub.getId());
        resp.setCompanyId(targetCompanyId);
        resp.setPlan(PlanResponse.fromEntity(currentPlan));
        resp.setStatus("ACTIVE");
        resp.setPreviousEndAt(previousEndAt);
        resp.setNewEndAt(newEndAt);
        resp.setDurationMonths(months);
        resp.setAmountPaid(amountToPay);
        resp.setPaymentProvider(payment.getProvider());
        resp.setTransactionRef(transactionRef);
        resp.setMessage(String.format("Gia hạn thành công gói %s thêm %d tháng! Hạn sử dụng mới đến ngày %s.",
                currentPlan.getName(), months, newEndAt.toLocalDate()));

        return resp;
    }

    @Override
    @Transactional
    public int processExpiredSubscriptions() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Subscription> expiredList = subscriptionRepository.findExpiredPaidSubscriptions(now);

        if (expiredList.isEmpty()) {
            log.info("Không có gói cước trả phí nào hết hạn cần xử lý tại thời điểm: {}", now);
            return 0;
        }

        log.info("Tìm thấy {} gói cước trả phí đã hết hạn cần xử lý...", expiredList.size());
        int processedCount = 0;

        Plan defaultFreePlan = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                .findFirst()
                .orElse(null);

        for (Subscription sub : expiredList) {
            try {
                Long companyId = sub.getCompany() != null ? sub.getCompany().getId() : null;
                String oldPlanName = sub.getPlan() != null ? sub.getPlan().getName() : "Unknown";

                Plan targetPlan = null;
                if (sub.getPendingPlanId() != null) {
                    targetPlan = planRepository.findById(sub.getPendingPlanId()).orElse(null);
                }

                if (targetPlan == null) {
                    targetPlan = defaultFreePlan;
                }

                if (targetPlan != null) {
                    sub.setPlan(targetPlan);
                    sub.setStatus("ACTIVE");
                    sub.setStartAt(now);
                    if (targetPlan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                        sub.setEndAt(null); // Gói free duy trì vĩnh viễn (không giới hạn thời gian)
                    } else {
                        sub.setEndAt(now.plusMonths(1));
                    }
                    sub.setPendingPlanId(null);
                    subscriptionRepository.save(sub);

                    // Xử lý tài nguyên kênh nếu gói mới có giới hạn kênh thấp hơn số kênh hiện tại của công ty
                    if (companyId != null && targetPlan.getMaxChannel() != null) {
                        List<vn.lnt.saas_customer_communication_platform.feature.channel.entity.Channel> channels =
                                channelRepository.findByCompanyId(companyId);
                        if (channels.size() > targetPlan.getMaxChannel()) {
                            int activeLimit = targetPlan.getMaxChannel();
                            int activeCount = 0;
                            for (var channel : channels) {
                                if (activeCount < activeLimit) {
                                    activeCount++;
                                } else {
                                    if (channel.getStatus() == vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus.ACTIVE) {
                                        channel.setStatus(vn.lnt.saas_customer_communication_platform.feature.channel.entity.ChannelStatus.INACTIVE);
                                        channelRepository.save(channel);
                                        log.info("Tạm khóa kênh [{}] do vượt hạn mức ({}) của gói mới [{}] cho công ty [{}]",
                                                channel.getName(), activeLimit, targetPlan.getName(), companyId);
                                    }
                                }
                            }
                        }
                    }

                    log.info("Đã xử lý hết hạn thành công cho công ty ID [{}]: chuyển từ gói [{}] sang gói [{}]",
                            companyId, oldPlanName, targetPlan.getName());
                    processedCount++;
                }
            } catch (Exception ex) {
                log.error("Lỗi khi xử lý hết hạn subscription ID [{}]: {}", sub.getId(), ex.getMessage(), ex);
            }
        }

        return processedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public int checkExpiringSubscriptions() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime in3Days = now.plusDays(3);
        List<Subscription> expiringSoon = subscriptionRepository.findExpiringPaidSubscriptions(now, in3Days);

        for (Subscription sub : expiringSoon) {
            Long companyId = sub.getCompany() != null ? sub.getCompany().getId() : null;
            String planName = sub.getPlan() != null ? sub.getPlan().getName() : "Unknown";
            long daysLeft = java.time.Duration.between(now, sub.getEndAt()).toDays();
            log.warn("CẢNH BÁO HẾT HẠN: Công ty ID [{}] đang dùng gói [{}] sẽ hết hạn sau {} ngày (vào ngày {}).",
                    companyId, planName, Math.max(0, daysLeft), sub.getEndAt().toLocalDate());
        }

        return expiringSoon.size();
    }
}

