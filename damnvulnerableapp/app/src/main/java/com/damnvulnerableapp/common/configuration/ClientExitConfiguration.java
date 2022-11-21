package com.damnvulnerableapp.common.configuration;


import com.damnvulnerableapp.networking.communication.client.Client;

/**
 * Configuration used when trying to exit a selected module or to shut down the app. It prevents
 * {@link Client#disconnect()} from blocking indefinitely.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ClientExitConfiguration extends ClientConfiguration {
    public ClientExitConfiguration() {
        this.endpointTimeout = 1000;
    }
}
