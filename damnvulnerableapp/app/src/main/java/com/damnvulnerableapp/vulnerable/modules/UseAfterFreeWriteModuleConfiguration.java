package com.damnvulnerableapp.vulnerable.modules;

public class UseAfterFreeWriteModuleConfiguration extends VulnerableConfiguration {

    public UseAfterFreeWriteModuleConfiguration() {

        this.endpointTimeout = 0;
        this.forwardTimeout = -1;
    }
}
