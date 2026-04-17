package com.example.applyjobs.exception;

public class NavigationException extends RuntimeException {
    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }
}

