package com.interestscsc.crawler.exceptions;

public class AccessDeniedException extends Exception {

    private String message;

    public AccessDeniedException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Access denied to url: " + message;
    }

    @Override
    public String toString() {
        return "Access denied to url: " + message;
    }
}
