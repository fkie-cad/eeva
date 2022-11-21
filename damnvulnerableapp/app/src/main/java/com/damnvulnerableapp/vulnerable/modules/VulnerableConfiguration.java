package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.networking.messages.PlainMessage;

/**
 * Data class that contains all mandatory information on how to run a vulnerable module. Primarily,
 * this class describes timeouts.
 *
 * Observe that this class is a subclass of {@link ClientConfiguration}. This allows for specifying
 * additional settings, like timeouts used for communicating with the manager service. As these
 * communication - related timeouts can vary among different vulnerable modules, each module should
 * be allowed to adjust them to their needs. It is used by {@link com.damnvulnerableapp.managerservice.controllers.states.VulnerableSelectState#select(PlainMessage)}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class VulnerableConfiguration extends ClientConfiguration {

    /**
     * Timeout for fetching data from the vulnerable module in milliseconds. At best, this should
     * be a non - zero number, because negative numbers will indicate infinite timeouts.
     * */
    protected int fetchTimeout;

    /**
     * Timeout for forwarding data to the vulnerable module in milliseconds. At best, this should
     * be a non - zero number, because negative numbers will indicate infinite timeouts.
     * */
    protected int forwardTimeout;

    /**
     * Timeout for outputting data from inside the vulnerable module in milliseconds. At best, this
     * should be a non - zero number, because negative numbers will indicate infinite timeouts.
     * */
    protected int outputTimeout;

    /**
     * Timeout for reading data from inside the vulnerable module in milliseconds. At best, this
     * should be a non - zero number, because negative numbers will indicate infinite timeouts.
     * */
    protected int inputTimeout;

    /**
     * Constructs this configuration by assigning default values to all timeouts.
     * */
    public VulnerableConfiguration() {

        this.fetchTimeout = 10000;
        this.forwardTimeout = 10000;
        this.outputTimeout = -1;
        this.inputTimeout = -1;
    }

    /**
     * Gets the timeout specified for fetching data from the vulnerable module. If this is negative,
     * then this MUST be interpreted as an infinite timeout.
     *
     * @return Timeout used for fetching data.
     * */
    public final int getFetchTimeout() {
        return this.fetchTimeout;
    }

    /**
     * Gets the timeout specified for forwarding data to the vulnerable module. If this is negative,
     * then this MUST be interpreted as an infinite timeout.
     *
     * @return Timeout used for forwarding data.
     * */
    public final int getForwardTimeout() {
        return this.forwardTimeout;
    }

    /**
     * Gets the timeout specified for outputting data from inside of the vulnerable module. If this
     * is negative, then this MUST be interpreted as an infinite timeout.
     *
     * @return Timeout used for outputting data from the perspective of the vulnerable module.
     * */
    public final int getOutputTimeout() {
        return this.outputTimeout;
    }

    /**
     * Gets the timeout specified for reading data from inside of the vulnerable module. If this is
     * negative, then this MUST be interpreted as an infinite timeout.
     *
     * @return Timeout used for reading data from the perspective of the vulnerable module.
     * */
    public final int getInputTimeout() {
        return this.inputTimeout;
    }
}
