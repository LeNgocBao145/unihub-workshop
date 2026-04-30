package org.unihubworkshop.workshopservice.exceptions;

public class InvalidWorkshopException extends RuntimeException {

    public InvalidWorkshopException(String message) {
        super(message);
    }

    public InvalidWorkshopException(String message, Throwable cause) {
        super(message, cause);
    }
}

