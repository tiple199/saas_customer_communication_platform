package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy;

import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderRequest;
import vn.lnt.saas_customer_communication_platform.feature.subscription.gateway.PaymentGatewayOrderResponse;

public interface PaymentGatewayStrategy {

    PaymentProvider getProvider();

    PaymentGatewayOrderResponse createPaymentOrder(PaymentGatewayOrderRequest request);

    boolean verifyWebhookSignature(String rawData, String signature);

    boolean isConfigured();
}

