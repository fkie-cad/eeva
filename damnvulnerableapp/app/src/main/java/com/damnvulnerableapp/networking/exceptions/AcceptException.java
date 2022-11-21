package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.server.NetworkServer;
import com.damnvulnerableapp.networking.communication.server.Server;

import java.net.ServerSocket;

/**
 * Exception that will be thrown, if {@link Server#accept()} encounters an error while accepting
 * an incoming exception. This is linked to the corresponding implementation of {@link Server#accept()},
 * i.e. e.g. {@link NetworkServer#accept()} will throw this exception, if {@link ServerSocket#accept()}
 * fails, i.e. throws {@link java.io.IOException}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class AcceptException extends CommunicationException {

    public AcceptException(String message) {
        super("Failed accepting connection: " + message);
    }

    public AcceptException(String message, Throwable cause) {
        super("Failed accepting connection: " + message, cause);
    }
}
