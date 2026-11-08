package vn.lnt.saas_customer_communication_platform.feature.subscription.service.impl;

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
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.PlanResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpdatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.Plan;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.PlanRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.repository.SubscriptionRepository;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public PlanServiceImpl(PlanRepository planRepository,
                           SubscriptionRepository subscriptionRepository,
                           UserRepository userRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
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
}

