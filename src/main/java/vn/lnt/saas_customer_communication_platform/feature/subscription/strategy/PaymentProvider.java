package vn.lnt.saas_customer_communication_platform.feature.subscription.strategy;

public enum PaymentProvider {
    VIETQR,
    SEPAY,
    STRIPE,
    SIMULATED;

    public static PaymentProvider fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            return SEPAY;
        }
        try {
            return PaymentProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SEPAY;
        }
    }
}

