package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PayOSPaymentStrategy implements PaymentGatewayStrategy {

    private static final Logger log = LoggerFactory.getLogger(PayOSPaymentStrategy.class);
    private static final String PAYOS_CREATE_PAYMENT_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    public static final String DEFAULT_BANK_CODE = "MB";
    public static final String DEFAULT_BANK_NAME = "Ngân hàng TMCP Quân đội (MB Bank)";
    public static final String DEFAULT_BANK_ACCOUNT = "0337335791";
    public static final String DEFAULT_ACCOUNT_NAME = "SAAS CUSTOMER COMMUNICATION PLATFORM";

    @Value("${payment.gateway.payos.client-id:}")
    private String clientId;

    @Value("${payment.gateway.payos.api-key:}")
    private String apiKey;

    @Value("${payment.gateway.payos.checksum-key:}")
    private String checksumKey;

    @Value("${payment.gateway.payos.return-url:http://localhost:3000/admin/plans}")
    private String defaultReturnUrl;

    @Value("${payment.gateway.payos.cancel-url:http://localhost:3000/admin/plans}")
    private String defaultCancelUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.PAYOS;
    }

    @Override
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && checksumKey != null && !checksumKey.isBlank();
    }

    @Override
    public PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request) {
        if (!isConfigured()) {
            log.info("[PayOS Strategy] Chưa cấu hình đầy đủ Client-ID / API-Key / Checksum-Key. Sử dụng chế độ VietQR Sandbox.");
            return createSandboxMockOrder(request);
        }

        try {
            long orderCode = request.getOrderCode();
            long amount = request.getAmount().longValue();
            String description = sanitizeDescription(request.getDescription(), orderCode);
            String returnUrl = (request.getReturnUrl() != null && !request.getReturnUrl().isBlank())
                    ? request.getReturnUrl() : defaultReturnUrl;
            String cancelUrl = (request.getCancelUrl() != null && !request.getCancelUrl().isBlank())
                    ? request.getCancelUrl() : defaultCancelUrl;

            String rawSignatureData = String.format(
                    "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    amount, cancelUrl, description, orderCode, returnUrl
            );
            String signature = hmacSha256(rawSignatureData, checksumKey);

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("orderCode", orderCode);
            bodyMap.put("amount", amount);
            bodyMap.put("description", description);
            bodyMap.put("returnUrl", returnUrl);
            bodyMap.put("cancelUrl", cancelUrl);
            bodyMap.put("signature", signature);

            String requestBody = objectMapper.writeValueAsString(bodyMap);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(PAYOS_CREATE_PAYMENT_URL))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", clientId.trim())
                    .header("x-api-key", apiKey.trim())
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                String code = root.path("code").asText("");
                if ("00".equals(code)) {
                    JsonNode data = root.path("data");
                    PaymentGatewayOrderResponse orderResp = new PaymentGatewayOrderResponse();
                    orderResp.setOrderCode(orderCode);
                    orderResp.setCheckoutUrl(data.path("checkoutUrl").asText(""));
                    orderResp.setQrCodeUrl(data.path("qrCode").asText(""));
                    orderResp.setAccountNumber(data.path("accountNumber").asText(""));
                    orderResp.setAccountName(data.path("accountName").asText(""));
                    orderResp.setBankCode(data.path("bin").asText(""));
                    orderResp.setBankName("Ngân hàng liên kết PayOS (BIN: " + data.path("bin").asText() + ")");
                    orderResp.setAmount(BigDecimal.valueOf(amount));
                    orderResp.setDescription(description);
                    orderResp.setRealGateway(true);

                    log.info("[PayOS Strategy] Tạo link thanh toán thành công cho đơn #{} (checkoutUrl={})",
                            orderCode, orderResp.getCheckoutUrl());
                    return orderResp;
                } else {
                    String desc = root.path("desc").asText("Lỗi từ PayOS");
                    log.warn("[PayOS Strategy] Gọi PayOS trả về code={}, desc={}. Fallback sang VietQR Sandbox.", code, desc);
                }
            } else {
                log.warn("[PayOS Strategy] Gọi PayOS HTTP error code={}. Body={}. Fallback sang VietQR Sandbox.",
                        response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.error("[PayOS Strategy] Ngoại lệ khi gọi PayOS: {}. Fallback sang VietQR Sandbox.", ex.getMessage(), ex);
        }

        return createSandboxMockOrder(request);
    }

    private PaymentGatewayOrderResponse createSandboxMockOrder(PaymentGatewayOrderRequest request) {
        long orderCode = request.getOrderCode();
        BigDecimal amount = request.getAmount();
        String description = "PAY" + orderCode;

        String encodedAccountName = URLEncoder.encode(DEFAULT_ACCOUNT_NAME, StandardCharsets.UTF_8);
        String encodedContent = URLEncoder.encode(description, StandardCharsets.UTF_8);
        String qrCodeUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                DEFAULT_BANK_CODE,
                DEFAULT_BANK_ACCOUNT,
                amount.toPlainString(),
                encodedContent,
                encodedAccountName
        );

        PaymentGatewayOrderResponse orderResp = new PaymentGatewayOrderResponse();
        orderResp.setOrderCode(orderCode);
        orderResp.setCheckoutUrl(defaultReturnUrl + "?orderCode=" + orderCode);
        orderResp.setQrCodeUrl(qrCodeUrl);
        orderResp.setBankCode(DEFAULT_BANK_CODE);
        orderResp.setBankName(DEFAULT_BANK_NAME);
        orderResp.setAccountNumber(DEFAULT_BANK_ACCOUNT);
        orderResp.setAccountName(DEFAULT_ACCOUNT_NAME);
        orderResp.setAmount(amount);
        orderResp.setDescription(description);
        orderResp.setRealGateway(false);
        return orderResp;
    }

    @Override
    public boolean verifyWebhookSignature(String rawData, String signature) {
        if (!isConfigured() || signature == null || signature.isBlank()) {
            return true;
        }
        try {
            String calculated = hmacSha256(rawData, checksumKey);
            return calculated.equalsIgnoreCase(signature.trim());
        } catch (Exception ex) {
            log.error("[PayOS Strategy] Lỗi khi xác thực chữ ký: {}", ex.getMessage());
            return false;
        }
    }

    private String sanitizeDescription(String desc, long orderCode) {
        if (desc == null || desc.isBlank()) {
            return "PAY" + orderCode;
        }
        String clean = desc.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (clean.length() > 25) {
            clean = clean.substring(0, 25).trim();
        }
        return clean.isBlank() ? ("PAY" + orderCode) : clean;
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC SHA256", e);
        }
    }
}

