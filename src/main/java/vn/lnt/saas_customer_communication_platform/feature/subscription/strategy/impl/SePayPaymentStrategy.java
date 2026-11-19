package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentGatewayStrategy;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SePayPaymentStrategy implements PaymentGatewayStrategy {

    private static final Logger log = LoggerFactory.getLogger(SePayPaymentStrategy.class);

    @Value("${payment.gateway.sepay.bank-code:BIDV}")
    private String bankCode;

    @Value("${payment.gateway.sepay.bank-name:Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (BIDV)}")
    private String bankName;

    @Value("${payment.gateway.sepay.account-number:SBSEPAY9HWFN4RKOSMM}")
    private String accountNumber;

    @Value("${payment.gateway.sepay.account-name:LE NHO TIEP}")
    private String accountName;

    @Value("${payment.gateway.sepay.payment-code-prefix:TP}")
    private String paymentCodePrefix;

    @Value("${payment.gateway.sepay.api-key:}")
    private String apiKey;

    @Value("${payment.gateway.sepay.webhook-secret:}")
    private String webhookSecret;

    @Value("${payment.gateway.sepay.return-url:http://localhost:3000/admin/plans}")
    private String defaultReturnUrl;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.SEPAY;
    }

    @Override
    public boolean isConfigured() {
        return accountNumber != null && !accountNumber.isBlank()
                && bankCode != null && !bankCode.isBlank();
    }

    public String getPaymentCodePrefix() {
        return (paymentCodePrefix != null && !paymentCodePrefix.isBlank()) ? paymentCodePrefix.trim() : "TP";
    }

    @Override
    public PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request) {
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        long orderCode = request.getOrderCode();
        String prefix = getPaymentCodePrefix();
        String transferMemo = prefix + orderCode;
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription() + " (" + transferMemo + ")"
                : transferMemo;

        String encodedDes = URLEncoder.encode(transferMemo, StandardCharsets.UTF_8);
        String encodedBank = URLEncoder.encode(bankCode.trim(), StandardCharsets.UTF_8);
        String encodedAcc = URLEncoder.encode(accountNumber.trim(), StandardCharsets.UTF_8);

        String sepayQrUrl = String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                encodedAcc,
                encodedBank,
                amount.longValue(),
                encodedDes
        );

        String returnUrl = request.getReturnUrl() != null && !request.getReturnUrl().isBlank()
                ? request.getReturnUrl()
                : defaultReturnUrl;

        log.info("[SePay Strategy] Đã khởi tạo mã QR SePay cho mã thanh toán={}, bank={}, acc={}",
                transferMemo, bankCode, accountNumber);

        PaymentGatewayOrderResponse resp = new PaymentGatewayOrderResponse();
        resp.setOrderCode(orderCode);
        resp.setAmount(amount);
        resp.setDescription(description);
        resp.setQrCodeUrl(sepayQrUrl);
        resp.setCheckoutUrl("https://my.sepay.vn/testmode/transaction/simulate");
        resp.setBankCode(bankCode);
        resp.setBankName(bankName);
        resp.setAccountNumber(accountNumber);
        resp.setAccountName(accountName);
        resp.setRealGateway((webhookSecret != null && !webhookSecret.isBlank())
                || (apiKey != null && !apiKey.isBlank()));
        return resp;
    }

    /**
     * Xác thực chữ ký HMAC-SHA256 từ SePay CLI / Webhook:
     * - Header X-SePay-Signature: sha256=<hex>
     * - Header X-SePay-Timestamp: <unix_seconds>
     * - Chuỗi ký: {timestamp}.{raw_body}
     */
    public boolean verifyHmacSignature(String rawBody, String timestamp, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }
        if (rawBody == null || timestamp == null || timestamp.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            String payloadToSign = timestamp.trim() + "." + rawBody;
            String expected = "sha256=" + hmacSha256(payloadToSign, webhookSecret.trim());
            return expected.equalsIgnoreCase(signatureHeader.trim());
        } catch (Exception e) {
            log.error("[SePay Strategy] Lỗi tính HMAC-SHA256: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawData, String signature) {
        // Nếu gửi kèm timestamp.rawBody ở rawData và sha256=... ở signature
        if (signature != null && signature.trim().startsWith("sha256=")
                && webhookSecret != null && !webhookSecret.isBlank()) {
            try {
                String expected = "sha256=" + hmacSha256(rawData, webhookSecret.trim());
                return expected.equalsIgnoreCase(signature.trim());
            } catch (Exception e) {
                log.error("[SePay Strategy] Lỗi xác thực chữ ký HMAC: {}", e.getMessage());
                return false;
            }
        }
        if (apiKey == null || apiKey.isBlank()) {
            return true;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String cleanHeader = signature.trim();
        if (cleanHeader.regionMatches(true, 0, "Apikey ", 0, 7)) {
            cleanHeader = cleanHeader.substring(7).trim();
        } else if (cleanHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            cleanHeader = cleanHeader.substring(7).trim();
        }
        return apiKey.trim().equals(cleanHeader);
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        byte[] bytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
