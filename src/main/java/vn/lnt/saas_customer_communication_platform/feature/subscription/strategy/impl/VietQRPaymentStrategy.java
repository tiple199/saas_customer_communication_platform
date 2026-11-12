package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentGatewayStrategy;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentProvider;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class VietQRPaymentStrategy implements PaymentGatewayStrategy {

    private static final Logger log = LoggerFactory.getLogger(VietQRPaymentStrategy.class);

    @Value("${payment.gateway.vietqr.bank-code:MB}")
    private String bankCode;

    @Value("${payment.gateway.vietqr.bank-name:Ngân hàng TMCP Quân đội (MB Bank)}")
    private String bankName;

    @Value("${payment.gateway.vietqr.account-number:0987654321}")
    private String accountNumber;

    @Value("${payment.gateway.vietqr.account-name:SAAS CUSTOMER COMMUNICATION PLATFORM}")
    private String accountName;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.VIETQR;
    }

    @Override
    public boolean isConfigured() {
        return accountNumber != null && !accountNumber.isBlank();
    }

    @Override
    public PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request) {
        log.info("[VietQR Strategy] Khởi tạo đơn thanh toán VietQR cho orderCode={}", request.getOrderCode());

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String memo = request.getDescription() != null ? request.getDescription() : "PAY_" + request.getOrderCode();

        String encodedMemo = URLEncoder.encode(memo, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        // Chuẩn định dạng ảnh VietQR qua QuickLink API
        String qrCodeUrl = String.format(
                "https://api.vietqr.io/image/%s-%s-compact2.jpg?amount=%s&addInfo=%s&accountName=%s",
                bankCode,
                accountNumber,
                amount.toPlainString(),
                encodedMemo,
                encodedAccountName
        );

        PaymentGatewayOrderResponse resp = new PaymentGatewayOrderResponse();
        resp.setOrderCode(request.getOrderCode());
        resp.setAmount(amount);
        resp.setDescription(memo);
        resp.setQrCodeUrl(qrCodeUrl);
        resp.setCheckoutUrl(qrCodeUrl);
        resp.setBankCode(bankCode);
        resp.setBankName(bankName);
        resp.setAccountNumber(accountNumber);
        resp.setAccountName(accountName);
        resp.setRealGateway(false);

        return resp;
    }

    @Override
    public boolean verifyWebhookSignature(String rawData, String signature) {
        // VietQR chuyển khoản ngân hàng thông thường không kèm signature đối soát cố định
        return true;
    }
}

