package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.server.Server;

/**
 * Exception that wraps all exceptions that are related to communication. This is used to properly
 * handle method signatures in abstract classes like {@link Server#accept()}. Exceptions thrown by
 * {@link Server#accept()} may be implementation specific. Therefore this wrapper is used to
 * avoid having to adjust method signatures for each new implementation of e.g. {@link Server}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class CommunicationException extends Exception {

    public CommunicationException(String message) {
        super("Communication failed: " + message);
    }

    public CommunicationException(String message, Throwable cause) {
        super("Communication failed: " + message, cause);
    }
}
