package com.damnvulnerableapp.common.configuration;

/**
 * Configurations for a {@link com.damnvulnerableapp.networking.communication.client.Client}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class ClientConfiguration extends Configuration {

    /**
     * Timeout for underlying {@link com.damnvulnerableapp.networking.communication.client.EndPoint}
     * in milliseconds. 0 indicates infinite timeout.
     * */
    protected int endpointTimeout = 5000;

    protected int handshakeTimeout = 2000;

    /**
     * Returns timeout for underlying {@link com.damnvulnerableapp.networking.communication.client.EndPoint}
     * in milliseconds.
     *
     * @return Timeout for {@link com.damnvulnerableapp.networking.communication.client.EndPoint}.
     * @see com.damnvulnerableapp.networking.communication.client.EndPoint
     * */
    public final int getEndpointTimeout() {
        return this.endpointTimeout;
    }

    public final int getHandshakeTimeout() {
        return this.handshakeTimeout;
    }
}
