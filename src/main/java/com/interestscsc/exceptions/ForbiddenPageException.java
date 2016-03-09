package com.interestscsc.exceptions;

public class ForbiddenPageException extends Exception {

    public ForbiddenPageException() {
         super();
    }

    public ForbiddenPageException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "Forbidden page's url: " + super.getMessage();
    }

    @Override
    public String toString() {
        return "Forbidden page's url: " + super.toString();
    }
}
