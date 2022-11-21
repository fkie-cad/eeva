package com.damnvulnerableapp.vulnerable.modules;

public class EasyStackBufferOverflowModuleConfiguration extends VulnerableConfiguration {

    public EasyStackBufferOverflowModuleConfiguration() {
        //this.endpointTimeout = 10000;
        this.endpointTimeout = 0;
        this.forwardTimeout = -1;
    }
}
