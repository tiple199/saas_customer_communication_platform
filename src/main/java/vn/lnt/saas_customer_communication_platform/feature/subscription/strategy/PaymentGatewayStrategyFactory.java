package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayStrategyFactory {

    private final Map<PaymentProvider, PaymentGatewayStrategy> strategyMap = new EnumMap<>(PaymentProvider.class);

    @Value("${payment.gateway.provider:PAYOS}")
    private String defaultProvider;

    public PaymentGatewayStrategyFactory(List<PaymentGatewayStrategy> strategies) {
        for (PaymentGatewayStrategy strategy : strategies) {
            strategyMap.put(strategy.getProvider(), strategy);
        }
    }

    public PaymentGatewayStrategy getStrategy(PaymentProvider provider) {
        if (provider == null) {
            provider = PaymentProvider.fromString(defaultProvider);
        }
        PaymentGatewayStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            strategy = strategyMap.get(PaymentProvider.PAYOS);
            if (strategy == null) {
                strategy = strategyMap.get(PaymentProvider.VIETQR);
            }
        }
        if (strategy == null) {
            throw new InvalidOperationException("Chưa hỗ trợ cổng thanh toán: " + provider);
        }
        return strategy;
    }

    public PaymentGatewayStrategy getStrategy(String providerName) {
        return getStrategy(PaymentProvider.fromString(providerName));
    }
}

