package vn.lnt.saas_customer_communication_platform.feature.subscription.service;

import org.springframework.data.domain.Pageable;
import vn.lnt.saas_customer_communication_platform.dto.PageResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.dto.*;

public interface PaymentService {

    PaymentOrderResponse createPaymentOrder(String currentUserEmail, Long companyId, CreatePaymentOrderRequest request);

    PaymentResponse confirmPayment(String currentUserEmail, ConfirmPaymentRequest request);

    PaymentResponse handlePaymentWebhook(PaymentWebhookRequest request);

    PaymentResponse handlePayOSWebhook(PayOSWebhookPayload payload);

    PaymentResponse cancelPaymentOrder(String currentUserEmail, String transactionRef);

    PaymentResponse getPaymentStatus(String currentUserEmail, String transactionRef);

    PageResponse<PaymentResponse> getCompanyPayments(String currentUserEmail, Long companyId, Pageable pageable);

    PaymentResponse getPaymentById(String currentUserEmail, Long paymentId);
}

