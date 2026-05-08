package org.unihubworkshop.paymentservice.exceptions;

public class SepayException extends RuntimeException {
    public SepayException(String message) {
        super(message);
    }

    public SepayException(String message, Throwable cause) {
        super(message, cause);
    }
}

