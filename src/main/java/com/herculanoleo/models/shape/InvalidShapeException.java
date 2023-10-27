package com.herculanoleo.models.shape;

public class InvalidShapeException extends RuntimeException {
    public InvalidShapeException(String message) {
        super(message);
    }

    public InvalidShapeException() {
        super();
    }
}
