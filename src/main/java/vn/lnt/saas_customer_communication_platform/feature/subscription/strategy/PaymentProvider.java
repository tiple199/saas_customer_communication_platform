package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy;

public enum PaymentProvider {
    PAYOS,
    VIETQR,
    VNPAY,
    MOMO,
    STRIPE,
    SIMULATED;

    public static PaymentProvider fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            return PAYOS;
        }
        try {
            return PaymentProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PAYOS;
        }
    }
}

