package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderResponse;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentGatewayStrategy;
import vn.lnt.saas_customer_communication_platform.feature.subscription.strategy.PaymentProvider;

import java.math.BigDecimal;

@Component
public class SimulatedPaymentStrategy implements PaymentGatewayStrategy {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentStrategy.class);

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.SIMULATED;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request) {
        log.info("[Simulated Strategy] Giả lập tạo đơn thanh toán test orderCode={}", request.getOrderCode());

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        String memo = request.getDescription() != null ? request.getDescription() : "SIMULATED_" + request.getOrderCode();

        PaymentGatewayOrderResponse resp = new PaymentGatewayOrderResponse();
        resp.setOrderCode(request.getOrderCode());
        resp.setAmount(amount);
        resp.setDescription(memo);
        resp.setQrCodeUrl("https://api.vietqr.io/image/970422-0987654321-compact2.jpg?amount=" + amount + "&addInfo=" + memo);
        resp.setCheckoutUrl("http://localhost:5173/admin/plans?mock_paid=" + request.getOrderCode());
        resp.setBankCode("SIMULATED");
        resp.setBankName("Ngân hàng Thử Nghiệm Sandbox");
        resp.setAccountNumber("0000000000");
        resp.setAccountName("SANDBOX TEST ACCOUNT");
        resp.setRealGateway(false);

        return resp;
    }

    @Override
    public boolean verifyWebhookSignature(String rawData, String signature) {
        return true;
    }
}

