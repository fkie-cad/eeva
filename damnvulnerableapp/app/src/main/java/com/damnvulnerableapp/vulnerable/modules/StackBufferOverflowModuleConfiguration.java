package com.damnvulnerableapp.vulnerable.modules;

/**
 * Configuration for {@link StackBufferOverflowModule}. It mainly uses the default timeouts set in
 * {@link VulnerableConfiguration}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class StackBufferOverflowModuleConfiguration extends VulnerableConfiguration {

    /**
     * Constructs configuration by setting the timeout for endpoints that represent connections to
     * external client (e.g. manager service) to a positive number.
     * */
    public StackBufferOverflowModuleConfiguration() {

        // No timeout at all -> might cause deadlocks
        //this.endpointTimeout = 10000;
        this.endpointTimeout = 0;
        this.forwardTimeout = -1;
    }
}
