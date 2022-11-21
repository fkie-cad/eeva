package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if any error related to the MVC architecture occurs. E.g. failing
 * to create important resources can produce this error.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see InitializationException
 * */
public class MVCException extends Exception {

    public MVCException(String message) {
        super("MVC error: " + message);
    }

    public MVCException(String message, Throwable e) {
        super("MVC error: " + message, e);
    }
}