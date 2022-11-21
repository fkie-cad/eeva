package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.server.BindInformation;

import java.net.SocketAddress;

/**
 * Exception that will be thrown, if {@link com.damnvulnerableapp.networking.communication.server.Server#bind(BindInformation)}
 * fails to bind to a specified location. This is linked to the respective implementation of
 * {@link com.damnvulnerableapp.networking.communication.server.Server#bind(BindInformation)}. E.g. {@link com.damnvulnerableapp.networking.communication.server.NetworkServer#bind(BindInformation)}
 * will throw this exception, if {@link java.net.ServerSocket#bind(SocketAddress)} fails by throwing
 * an {@link java.io.IOException}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class BindException extends CommunicationException {

    public BindException(String message) {
        super("Failed to bind: " + message);
    }

    public BindException(String message, Throwable cause) {
        super("Failed to bind: " + message, cause);
    }
}
