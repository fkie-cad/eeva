package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;
import com.damnvulnerableapp.networking.communication.server.Server;

/**
 * Exception that will be thrown, if e.g. accepting a client connection via {@link Server#accept()},
 * connecting to a server via {@link Client#connect(ConnectionInformation)}
 * or receiving a message via {@link Client#receive()} times out. Timeouts often are specified in
 * configurations.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class TimeoutException extends CommunicationException {

    public TimeoutException(String message) {
        super("Timed out: " + message);
    }

    public TimeoutException(String message, Throwable cause) {
        super("Timed out: " + message, cause);
    }
}
