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
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class VNPayPaymentStrategy implements PaymentGatewayStrategy {

    private static final Logger log = LoggerFactory.getLogger(VNPayPaymentStrategy.class);

    @Value("${payment.gateway.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${payment.gateway.vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${payment.gateway.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${payment.gateway.vnpay.return-url:http://localhost:3000/admin/plans}")
    private String defaultReturnUrl;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.VNPAY;
    }

    @Override
    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank()
                && hashSecret != null && !hashSecret.isBlank();
    }

    @Override
    public PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request) {
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        long orderCode = request.getOrderCode();
        String description = request.getDescription() != null ? request.getDescription() : "PAY_" + orderCode;

        if (!isConfigured()) {
            log.info("[VNPay Strategy] Chưa cấu hình TMN-Code / Hash-Secret. Dùng Mock VNPay Sandbox.");
            PaymentGatewayOrderResponse resp = new PaymentGatewayOrderResponse();
            resp.setOrderCode(orderCode);
            resp.setAmount(amount);
            resp.setDescription(description);
            resp.setQrCodeUrl("https://api.vietqr.io/image/970422-0987654321-compact2.jpg?amount=" + amount + "&addInfo=VNPAY_" + orderCode);
            resp.setCheckoutUrl(defaultReturnUrl + "?vnp_ResponseCode=00&vnp_TxnRef=" + orderCode);
            resp.setBankCode("VNPAY");
            resp.setBankName("Cổng Thanh Toán VNPay");
            resp.setAccountNumber("VNPAY_" + orderCode);
            resp.setAccountName("VNPAY GATEWAY");
            resp.setRealGateway(false);
            return resp;
        }

        try {
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", tmnCode);
            // VNPay yêu cầu nhân 100 cho đơn vị tiền tệ VND
            long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
            vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", String.valueOf(orderCode));
            vnpParams.put("vnp_OrderInfo", description);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", request.getReturnUrl() != null && !request.getReturnUrl().isBlank()
                    ? request.getReturnUrl() : defaultReturnUrl);
            vnpParams.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Xây dựng query string & hash data
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<Map.Entry<String, String>> itr = vnpParams.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isBlank()) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String secureHash = hmacSha512(hashData.toString(), hashSecret);
            String paymentUrl = vnpPayUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

            PaymentGatewayOrderResponse resp = new PaymentGatewayOrderResponse();
            resp.setOrderCode(orderCode);
            resp.setAmount(amount);
            resp.setDescription(description);
            resp.setCheckoutUrl(paymentUrl);
            resp.setQrCodeUrl("https://api.vietqr.io/image/970422-0987654321-compact2.jpg?amount=" + amount + "&addInfo=VNPAY_" + orderCode);
            resp.setBankCode("VNPAY");
            resp.setBankName("Cổng Thanh Toán VNPay");
            resp.setAccountNumber("VNPAY_" + orderCode);
            resp.setAccountName("VNPAY GATEWAY");
            resp.setRealGateway(true);

            return resp;
        } catch (Exception e) {
            log.error("[VNPay Strategy] Lỗi tạo URL thanh toán VNPay: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo thanh toán VNPay: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawData, String signature) {
        if (!isConfigured() || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String calculated = hmacSha512(rawData, hashSecret);
            return calculated.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("[VNPay Strategy] Lỗi xác thực chữ ký VNPay: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSha512(String data, String key) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(secretKey);
        byte[] hash = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

