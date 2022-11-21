package com.damnvulnerableapp.common.configuration;

import com.damnvulnerableapp.networking.communication.server.Server;

/**
 * Configuration for a {@link com.damnvulnerableapp.networking.communication.server.Server}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class ServerConfiguration extends Configuration {

    /**
     * Timeout for e.g. {@link Server#accept()}. In case of {@link com.damnvulnerableapp.networking.communication.server.NetworkServer}
     * this value will be used in {@link java.net.ServerSocket#setSoTimeout(int)}. A timeout
     * of 0 indicates no timeout. Defaults to 0ms.
     * */
    protected int timeout = 0;

    /**
     * Total number of clients to accept. Setting this to 0 indicates that infinite clients may
     * connect.
     * */
    protected int numClients = 0;

    /**
     * Delay for accept loop in case {@link Server#startAsync()} is used. Defaults to 50ms.
     * */
    protected int acceptLoopDelay = 50;

    /**
     * Returns amount of milliseconds to wait for a client connection.
     *
     * @return Timeout.
     * */
    public final int getTimeout() {
        return this.timeout;
    }

    /**
     * Returns amount of client connections to accept (sequentially, not in parallel).
     *
     * @return Number of allowed client connections.
     * */
    public final int getNumClients() {
        return this.numClients;
    }

    /**
     * Returns amount of milliseconds to wait between each accept - loop iteration in case
     * {@link Server#startAsync()} is used.
     *
     * @return Accept - loop delay.
     * */
    public final int getAcceptLoopDelay() {
        return this.acceptLoopDelay;
    }
}
