package com.damnvulnerableapp.networking.communication.server;

/**
 * Information on where to bind a {@link java.net.ServerSocket}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkBindInformation extends BindInformation {

    /**
     * Host name, on which to bind the {@link java.net.ServerSocket}.
     * */
    private final String host;

    /**
     * Port of the {@link java.net.ServerSocket}.
     * */
    private final int port;

    /**
     * Assigns binding information.
     *
     * @param host Host name, on which to bind.
     * @param port Port, on which to bind. Only the least - significant 2 bytes are considered.
     * */
    public NetworkBindInformation(String host, int port) {
        this.host = host;
        this.port = (port & 0xffff);
    }

    /**
     * Returns the host name, on which a {@link java.net.ServerSocket} should bind.
     *
     * @return Host name.
     * */
    public String getHost() {
        return this.host;
    }

    /**
     * Returns the port, on which a {@link java.net.ServerSocket} should bind. Only the least -
     * significant 2 bytes are considered.
     *
     * @return Port.
     * */
    public int getPort() {
        return this.port;
    }
}
