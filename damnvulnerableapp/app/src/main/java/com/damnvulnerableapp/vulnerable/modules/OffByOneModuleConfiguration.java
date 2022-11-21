package com.damnvulnerableapp.vulnerable.modules;

public class OffByOneModuleConfiguration extends VulnerableConfiguration {

    public OffByOneModuleConfiguration() {

        this.endpointTimeout = 0;
        this.forwardTimeout = -1;
    }
}
