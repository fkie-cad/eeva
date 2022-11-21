package com.damnvulnerableapp.networking.communication.client;

import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;

/**
 * Implementation of a client that is based on {@link java.net.Socket}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkClient extends Client {

    /**
     * Construct socket - based client. This constructor is used by {@link NetworkFactory#createClient()}
     * to create new instances of socket - based clients.
     *
     * Do NOT call this constructor directly. Rather call {@link NetworkFactory#createClient()}
     * to obtain an instance of {@link NetworkClient}. Calling this constructor anyways may result
     * in unstable connections, if any.
     *
     * @see Client#Client()
     * */
    public NetworkClient() {
        super();
    }

    /**
     * Construct socket - based client, where the internal implementation is already created. This
     * will be useful only in conjunction with a {@link com.damnvulnerableapp.networking.communication.server.NetworkServer}.
     *
     * Do NOT call this constructor directly. Rather call {@link NetworkFactory#createClient()}
     * to obtain an instance of {@link NetworkClient}. Calling this constructor anyways may result
     * in unstable connections, if any.
     *
     * @param endpoint Socket - based implementation of communication.
     * */
    public NetworkClient(EndPoint endpoint) {
        super(endpoint);
    }

    /**
     * Creates a new instance of a socket - based implementation of communication. This will be
     * called by {@link Client#connect(ConnectionInformation)}, if there is no connected endpoint
     * yet.
     *
     * @return New instance of {@link NetworkEndPoint}.
     * @throws ConnectionException If establishing a connection fails. This should only happen, if
     *                             the {@link EndPoint} is used in conjunction with a
     *                             {@link com.damnvulnerableapp.networking.communication.server.Server}.
     * */
    @Override
    protected EndPoint createEndPoint() throws ConnectionException {
        return new NetworkEndPoint();
    }
}
