package com.interestscsc.exceptions;

/**
 * Created by Maxim on 17.03.2016.
 */
public class NotFoundPageException extends Exception {

    public NotFoundPageException() {
        super();
    }

    public NotFoundPageException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "Pages on website: " + super.getMessage() + " not found!";
    }

    @Override
    public String toString() {
        return "Pages on website: " + super.toString() + " not found!";
    }
}
