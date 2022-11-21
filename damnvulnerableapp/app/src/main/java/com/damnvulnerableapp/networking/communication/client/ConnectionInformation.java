package com.damnvulnerableapp.networking.communication.client;

/**
 * Common superclass of all data classes wrapping connection information.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class ConnectionInformation {

    private final int timeout;

    public ConnectionInformation(int timeout) {

        this.timeout = timeout;
    }

    public int getTimeout() {
        return this.timeout;
    }
}
