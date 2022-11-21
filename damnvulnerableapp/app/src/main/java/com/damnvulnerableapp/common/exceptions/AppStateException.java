package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if an error related to the internal app(lication) state occurs.
 * E.g. trying to move to a state, but there is no supported transition, may cause this error.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class AppStateException extends Exception {

    public AppStateException(String message) {
        super("Error with state of app: " + message);
    }

    public  AppStateException(String message, Throwable e) {
        super("Error with state of app: " + message, e);
    }
}
