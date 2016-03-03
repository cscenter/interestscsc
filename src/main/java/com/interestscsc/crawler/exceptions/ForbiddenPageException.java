package com.interestscsc.crawler.exceptions;

public class ForbiddenPageException extends Exception {

    private String message;

    public ForbiddenPageException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Forbidden page's url: " + message;
    }

    @Override
    public String toString() {
        return "Forbidden page's url: " + message;
    }
}
