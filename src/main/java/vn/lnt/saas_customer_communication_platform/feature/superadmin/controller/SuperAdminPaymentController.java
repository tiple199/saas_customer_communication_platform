package vn.lnt.saas_customer_communication_platform.feature.superadmin.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentItemDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.dto.SuperAdminPaymentStatsDto;
import vn.lnt.saas_customer_communication_platform.feature.superadmin.service.SuperAdminPaymentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/super-admin/payments")
public class SuperAdminPaymentController {

    private final SuperAdminPaymentService paymentService;

    public SuperAdminPaymentController(SuperAdminPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SuperAdminPaymentItemDto>>> getPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String currentUserEmail = jwt.getSubject();
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        PageResponse<SuperAdminPaymentItemDto> response = paymentService.getPayments(
                currentUserEmail, keyword, status, provider, fromDateTime, toDateTime, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giao dịch thành công", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SuperAdminPaymentStatsDto>> getPaymentStats(
            @AuthenticationPrincipal Jwt jwt) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminPaymentStatsDto stats = paymentService.getPaymentStats(currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê giao dịch thành công", stats));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<SuperAdminPaymentItemDto>> confirmManualPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminPaymentItemDto response = paymentService.confirmManualPayment(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thanh toán thủ công thành công", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SuperAdminPaymentItemDto>> cancelManualPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        String currentUserEmail = jwt.getSubject();
        SuperAdminPaymentItemDto response = paymentService.cancelManualPayment(currentUserEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giao dịch thành công", response));
    }
}

