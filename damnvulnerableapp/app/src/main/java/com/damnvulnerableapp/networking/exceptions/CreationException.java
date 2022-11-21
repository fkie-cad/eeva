package com.damnvulnerableapp.networking.exceptions;

/**
 * Exception that will be thrown, if creating a communication - related resource fails. E.g.
 * creating a {@link java.net.ServerSocket} in {@link com.damnvulnerableapp.networking.communication.server.NetworkServer}
 * may throw an {@link java.io.IOException}, which will be translated into this exception.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class CreationException extends CommunicationException {

    public CreationException(String message) {
        super("Failed to create resource: " + message);
    }

    public CreationException(String message, Throwable cause) {
        super("Failed to create resource: " + message, cause);
    }
}
