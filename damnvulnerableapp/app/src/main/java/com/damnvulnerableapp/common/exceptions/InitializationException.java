package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if initializing a resource failed and cannot be recovered. E.g.
 * failing to create a {@link com.damnvulnerableapp.networking.communication.client.Client} for
 * communication with a vulnerable module would be devastating, therefore causing this error.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class InitializationException extends MVCException {

    public InitializationException(String message) {
        super("Initialization failed: " + message);
    }

    public InitializationException(String message, Throwable e) {
        super("Initialization failed: " + message, e);
    }
}
