package vn.lnt.saas_customer_communication_platform.feature.subscription.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.subscription.service.PaymentService;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyIdHeader,
            @RequestParam(value = "companyId", required = false) Long companyIdParam,
            @Valid @RequestBody CreatePaymentOrderRequest request) {

        String currentUserEmail = jwt.getSubject();
        Long companyId = companyIdParam != null ? companyIdParam : companyIdHeader;
        PaymentOrderResponse response = paymentService.createPaymentOrder(currentUserEmail, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo đơn hàng thanh toán thành công", response));
    }

    @PostMapping("/payments/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        String currentUserEmail = jwt.getSubject();
        PaymentResponse response = paymentService.confirmPayment(currentUserEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công", response));
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<ApiResponse<PaymentResponse>> handlePaymentWebhook(
            @RequestBody PaymentWebhookRequest request) {

        PaymentResponse response = paymentService.handlePaymentWebhook(request);
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý thông báo thanh toán Webhook", response));
    }

    @PostMapping("/payments/webhook/payos")
    public ResponseEntity<ApiResponse<PaymentResponse>> handlePayOSWebhook(
            @RequestBody PayOSWebhookPayload payload) {

        PaymentResponse response = paymentService.handlePayOSWebhook(payload);
        return ResponseEntity.ok(ApiResponse.success("PayOS Webhook đã xử lý thành công", response));
    }

    @PostMapping("/payments/{transactionRef}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPaymentOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String transactionRef) {

        String currentUserEmail = jwt.getSubject();
        PaymentResponse response = paymentService.cancelPaymentOrder(currentUserEmail, transactionRef);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn giao dịch thanh toán", response));
    }

    @GetMapping("/payments/check-status/{transactionRef}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String transactionRef) {

        String currentUserEmail = jwt.getSubject();
        PaymentResponse response = paymentService.getPaymentStatus(currentUserEmail, transactionRef);
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái giao dịch thành công", response));
    }

    @GetMapping("/companies/{companyId}/payments")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getCompanyPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long companyId,
            Pageable pageable) {

        String currentUserEmail = jwt.getSubject();
        PageResponse<PaymentResponse> response = paymentService.getCompanyPayments(currentUserEmail, companyId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thanh toán thành công", response));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long paymentId) {

        String currentUserEmail = jwt.getSubject();
        PaymentResponse response = paymentService.getPaymentById(currentUserEmail, paymentId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết giao dịch thành công", response));
    }
}
