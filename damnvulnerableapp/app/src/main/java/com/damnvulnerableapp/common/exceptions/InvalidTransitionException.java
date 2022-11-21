package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if someone tries to move from one state to another without a
 * supported transition. E.g. being in {@link com.damnvulnerableapp.managerservice.controllers.states.VulnerableSelectState},
 * one cannot EXIT and thus re-enter the SELECT state, because there is no running module to exit.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class InvalidTransitionException extends AppStateException {

    public InvalidTransitionException(String message) {
        super("Failed to transition to new state: " + message);
    }

    public InvalidTransitionException(String message, Throwable e) {
        super("Failed to transition to new state: " + message, e);
    }
}
