package com.damnvulnerableapp.test.helpers;

import com.damnvulnerableapp.common.configuration.ServerConfiguration;

public class DynamicServerConfiguration extends ServerConfiguration {

    public DynamicServerConfiguration(int timeout, int numClients, int acceptLoopDelay) {

        this.timeout = timeout;
        this.numClients = numClients;
        this.acceptLoopDelay = acceptLoopDelay;
    }
}
