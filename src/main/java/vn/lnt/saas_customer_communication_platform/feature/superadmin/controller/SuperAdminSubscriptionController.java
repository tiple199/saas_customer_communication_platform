package vn.lnt.saas_customer_communication_platform.feature.superadmin.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.ExtendSubscriptionRequest;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminSubscriptionStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminSubscriptionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/subscriptions")
public class SuperAdminSubscriptionController {

    private final SuperAdminSubscriptionService subscriptionService;

    public SuperAdminSubscriptionController(SuperAdminSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SuperAdminSubscriptionItemDto>>> getSubscriptions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) Boolean isExpiringSoon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String currentUserEmail = jwt.getSubject();
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        PageResponse<SuperAdminSubscriptionItemDto> response = subscriptionService.getSubscriptions(
                currentUserEmail, keyword, status, planId, isPaid, isExpiringSoon, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thuê bao thành công", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SuperAdminSubscriptionStatsDto>> getSubscriptionStats(
            @AuthenticationPrincipal Jwt jwt) {
        String currentUserEmail = jwt.getSubject();
        SuperAdminSubscriptionStatsDto stats = subscriptionService.getSubscriptionStats(currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thuê bao thành công", stats));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<SuperAdminSubscriptionItemDto>> extendSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ExtendSubscriptionRequest request) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminSubscriptionItemDto response = subscriptionService.extendSubscription(currentUserEmail, id, request);
        return ResponseEntity.ok(ApiResponse.success("Gia hạn thuê bao thành công", response));
    }

    @PostMapping("/{id}/cancel-to-free")
    public ResponseEntity<ApiResponse<SuperAdminSubscriptionItemDto>> cancelToFree(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String currentUserEmail = jwt.getSubject();
        String reason = body != null ? body.getOrDefault("reason", "Super Admin chuyển về gói Starter")
                : "Super Admin chuyển về gói Starter";

        SuperAdminSubscriptionItemDto response = subscriptionService.cancelToFree(currentUserEmail, id, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển thuê bao về gói miễn phí (Starter)", response));
    }
}

