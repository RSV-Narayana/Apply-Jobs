package com.example.applyjobs.exception;

public class JobExtractionException extends RuntimeException {
    public JobExtractionException(String message) {
        super(message);
    }

    public JobExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

