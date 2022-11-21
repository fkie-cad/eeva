package com.damnvulnerableapp.networking.communication.client;

/**
 * Wrapper for connection information that is used for connecting a {@link java.net.Socket} to
 * a {@link java.net.ServerSocket}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkConnectionInformation extends ConnectionInformation {

    /**
     * Host name to connect to. Most of the time, this is an IPv4 address.
     * */
    private final String host;

    /**
     * Port number to connect to. This implies usage of TCP.
     * */
    private final int port;

    /**
     * Constructs data object by assigning the connection destination.
     *
     * @param host Host to connect to. The user of this class has to ensure that the host name is
     *             valid.
     * @param port Port to connect to. Only least - significant 2 bytes are considered.
     * @param timeout Timeout used for connecting to server.
     * */
    public NetworkConnectionInformation(String host, int port, int timeout) {
        super(timeout);
        this.host = host;
        this.port = (port & 0xffff);
    }

    /**
     * Returns the host name, to which a socket is supposed to connect.
     *
     * @return Host name.
     * */
    public String getHost() {
        return this.host;
    }

    /**
     * Returns the port, to which a socket is supposed to connect. Only the least - significant
     * 2 bytes are returned.
     *
     * @return Port.
     * */
    public int getPort() {

        return this.port;
    }
}
