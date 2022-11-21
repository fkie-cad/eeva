package com.damnvulnerableapp.networking.exceptions;

/**
 * Exception that will be thrown, if a client fails to properly authenticate with the server. This
 * can be due to the fact that a client posts an incompatible functionality to a server. To that end
 * notice that each client is assigned a functionality that roughly describes what this client wants
 * to do on the server. If the server does not support the client's functionality, this will trigger
 * this exception.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class InvalidClientException extends HandshakeException {

    public InvalidClientException(String message) {
        super("Invalid client: " + message);
    }

    public InvalidClientException(String message, Throwable cause) {
        super("Invalid client: " + message, cause);
    }
}
