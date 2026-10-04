package vn.lnt.saas_customer_communication_platform.exception;

import java.io.Serial;

public class InvalidOperationException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidOperationException(String message) {
        super(message);
    }
}
