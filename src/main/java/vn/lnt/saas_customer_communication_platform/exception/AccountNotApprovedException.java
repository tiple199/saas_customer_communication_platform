package vn.lnt.saas_customer_communication_platform.exception;

import java.io.Serial;

public class AccountNotApprovedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AccountNotApprovedException(String message) {
        super(message);
    }
}
