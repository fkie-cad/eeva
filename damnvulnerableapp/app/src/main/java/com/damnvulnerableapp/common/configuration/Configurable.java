package com.damnvulnerableapp.common.configuration;

import androidx.annotation.NonNull;

/**
 * Defines how configuration - related operations are to be performed.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public interface Configurable {

    /**
     * Sets a new configuration by overwriting the old one. Implementing this function allows for
     * dynamically adjusting properties of objects that depend on the configurations. E.g. the
     * {@link com.damnvulnerableapp.networking.communication.client.NetworkEndPoint} can set its
     * reading - timeout whenever a new configuration is to be used.
     *
     * @param configuration New configuration to use.
     * @see Configuration
     * */
    void setConfiguration(Configuration configuration);

    /**
     * Gets currently selected configuration, or a modified version of it, depending on the use - case.
     *
     * @return Currently selected configuration.
     * @see Configuration
     * */
    @NonNull
    Configuration getConfiguration();
}
