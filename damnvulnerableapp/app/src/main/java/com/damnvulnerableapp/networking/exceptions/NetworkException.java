package com.damnvulnerableapp.networking.exceptions;

/**
 * Exception that wraps network - based, i.e. socket - based exceptions. This may become useful
 * in the future.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkException extends CommunicationException {

    public NetworkException(String message) {
        super("Networking error: " + message);
    }

    public NetworkException(String message, Throwable cause) {
        super("Networking error: " + message, cause);
    }
}
