package com.damnvulnerableapp.networking.exceptions;

/**
 * Exception that will be thrown, if an internal protocol error is triggered. E.g. {@link java.net.ServerSocket#setSoTimeout(int)}
 * may fail due to internal TCP errors.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class InternalSocketException extends NetworkException {

    public InternalSocketException(String message) {
        super("Internal socket error: " + message);
    }

    public InternalSocketException(String message, Throwable cause) {
        super("Internal socket error: " + message, cause);
    }
}
