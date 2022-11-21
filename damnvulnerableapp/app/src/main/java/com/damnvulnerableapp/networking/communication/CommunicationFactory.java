package com.damnvulnerableapp.networking.communication;

import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.protocol.ProtocolFactory;

/**
 * A class that constructs clients and servers that share a common interface and technical
 * implementation. The protocol used by client and server is determined by calling
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class CommunicationFactory {

    /**
     * Factory for constructing a common protocol that is used by client and server.
     * @see ProtocolFactory
     * */
    private ProtocolFactory protocolFactory;

    /**
     * Fixes a protocol creator to use when creating client and server objects. Every {@link Client}
     * and {@link Server} instance will be given an instance of the {@link com.damnvulnerableapp.networking.protocol.Protocol}
     * that is returned by <code>protocolFactory</code>.
     *
     * Notice that there are no guarantees regarding compatibility among two different protocols.
     * @param protocolFactory Protocol creator that will be used to dynamically create instances of
     *                        {@link com.damnvulnerableapp.networking.protocol.Protocol}.
     * @see ProtocolFactory
     * */
    public void setProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Returns currently selected {@link ProtocolFactory} that is used to create instances of
     * {@link com.damnvulnerableapp.networking.protocol.Protocol}.
     * @return Instance of selected {@link ProtocolFactory}; or null, if this is called before
     *         {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}.
     * @see ProtocolFactory
     * */
    public ProtocolFactory getProtocolFactory() {
        return this.protocolFactory;
    }

    /**
     * Instantiates an implementation of {@link Client}. This can be used to send requests to a
     * corresponding {@link Server}. Notice that by calling {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}
     * in between {@link CommunicationFactory#createClient()} and {@link CommunicationFactory#createServer()}
     * can result in incompatible clients and servers.
     * @return Instance of an implementation of {@link Client}.
     * @see Client
     * */
    public abstract Client createClient();

    /**
     * Instantiates an implementation of {@link Server}. This instance can be used to handle client
     * requests. Notice that by calling {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}
     * in between {@link CommunicationFactory#createClient()} and {@link CommunicationFactory#createServer()}
     * can result in incompatible clients and servers.
     * @return Instance of an implementation of {@link Server}.
     * @see Server
     * */
    public abstract Server createServer();
}
