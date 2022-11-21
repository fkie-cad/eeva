package com.damnvulnerableapp.test.helpers;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;

public class DynamicClientConfiguration extends ClientConfiguration {

    public DynamicClientConfiguration(int endpointTimeout) {
        this.endpointTimeout = endpointTimeout;
    }
}
