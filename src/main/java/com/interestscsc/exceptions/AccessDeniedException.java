package com.interestscsc.exceptions;

public class AccessDeniedException extends Exception {

    public AccessDeniedException() {
        super();
    }

    public AccessDeniedException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "Access denied to url: " + super.getMessage();
    }

    @Override
    public String toString() {
        return "Access denied to url: " + super.toString();
    }
}
