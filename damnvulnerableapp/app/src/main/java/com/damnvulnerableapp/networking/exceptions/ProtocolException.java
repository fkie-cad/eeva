package com.damnvulnerableapp.networking.exceptions;

/**
 * Exception that will be thrown, if a protocol - related error occurs. This is a wrapper for all
 * protocol - related errors.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class ProtocolException extends CommunicationException {

    public ProtocolException(String message) {
        super("Protocol error: " + message);
    }

    public ProtocolException(String message, Throwable cause) {
        super("Protocol error: " + message, cause);
    }
}
