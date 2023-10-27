package com.herculanoleo.models.exception;

public class InvalidShapeException extends RuntimeException {
    public InvalidShapeException(String message) {
        super(message);
    }

    public InvalidShapeException() {
        super();
    }
}
