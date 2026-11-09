package vn.lnt.saas_customer_communication_platform.feature.subscription.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CreatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.PlanResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpdatePlanRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpdatePlanStatusRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.entity.PlanStatus;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PlanService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePlanRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PlanResponse response = planService.createPlan(currentUserEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo gói dịch vụ thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getAllPlans(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) PlanStatus status) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        List<PlanResponse> response = planService.getAllPlans(currentUserEmail, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói dịch vụ thành công", response));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<PlanResponse>>> getPlansPaged(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) PlanStatus status,
            @PageableDefault(size = 20, sort = "price", direction = Sort.Direction.ASC) Pageable pageable) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PageResponse<PlanResponse> response = planService.getPlansPaged(currentUserEmail, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói dịch vụ phân trang thành công", response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getActivePlans() {
        List<PlanResponse> response = planService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói dịch vụ đang hoạt động thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PlanResponse response = planService.getPlanById(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin gói dịch vụ thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PlanResponse response = planService.updatePlan(currentUserEmail, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật gói dịch vụ thành công", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlanStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanStatusRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        PlanResponse response = planService.updatePlanStatus(currentUserEmail, id, request.getStatus());
        String message = request.getStatus() == PlanStatus.ACTIVE
                ? "Kích hoạt gói dịch vụ thành công"
                : "Ngừng kích hoạt gói dịch vụ thành công";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        planService.deletePlan(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa gói dịch vụ thành công", null));
    }

    @GetMapping("/current-subscription")
    public ResponseEntity<ApiResponse<vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CompanySubscriptionResponse>> getCompanyCurrentSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        var response = planService.getCompanyCurrentSubscription(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin gói dịch vụ công ty đang sử dụng thành công", response));
    }

    @GetMapping("/calculate-upgrade")
    public ResponseEntity<ApiResponse<vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeCalculationResponse>> calculateUpgrade(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @RequestParam Long targetPlanId,
            @RequestParam(defaultValue = "1") Integer durationMonths) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        var response = planService.calculateUpgradeAmount(currentUserEmail, companyId, targetPlanId, durationMonths);
        return ResponseEntity.ok(ApiResponse.success("Tính toán chi phí nâng cấp thành công", response));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionResponse>> upgradeSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody vn.lnt.saas_customer_communication_platform.feature.subscription.dto.UpgradeSubscriptionRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        var response = planService.upgradeCompanySubscription(currentUserEmail, companyId, request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/downgrade")
    public ResponseEntity<ApiResponse<vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionResponse>> requestDowngrade(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId,
            @Valid @RequestBody vn.lnt.saas_customer_communication_platform.feature.subscription.dto.DowngradeSubscriptionRequest request) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        var response = planService.requestDowngradeSubscription(currentUserEmail, companyId, request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @DeleteMapping("/downgrade/cancel")
    public ResponseEntity<ApiResponse<vn.lnt.saas_customer_communication_platform.feature.subscription.dto.CompanySubscriptionResponse>> cancelPendingDowngrade(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long companyId) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        var response = planService.cancelPendingDowngrade(currentUserEmail, companyId);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu hạ cấp gói cước thành công", response));
    }
}

