package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;

import java.net.SocketAddress;

/**
 * Exception that will be thrown, if any connection - related error occurs. E.g. if establishing
 * a connection fails due to {@link com.damnvulnerableapp.networking.communication.client.NetworkEndPoint#connect(ConnectionInformation)}
 * being unable to execute {@link java.net.Socket#connect(SocketAddress, int)}. This can also be
 * thrown in response to unexpected connection shutdowns, e.g. in response to an {@link java.io.IOException}
 * that is thrown by {@link java.io.DataInputStream#readFully(byte[], int, int)}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class ConnectionException extends CommunicationException {

    public ConnectionException(String message) {
        super("Connection error: " + message);
    }

    public ConnectionException(String message, Throwable cause) {
        super("Connection error: " + message, cause);
    }
}
