package com.damnvulnerableapp.networking.communication;

import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.NetworkClient;
import com.damnvulnerableapp.networking.communication.server.NetworkServer;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.protocol.ProtocolFactory;

/**
 * Factory producing socket - based clients and servers that speak a common protocol. For the
 * latter it is assumed that there is no call to {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}
 * in between a call to {@link NetworkFactory#createClient()} and {@link NetworkFactory#createServer()}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see CommunicationFactory
 * */
public class NetworkFactory extends CommunicationFactory{

    /**
     * Creates an instance of a socket - based client. Also, using this {@link ProtocolFactory} set
     * via {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}, the client will be
     * given a {@link com.damnvulnerableapp.networking.protocol.Protocol} such that it can
     * communicate with a server that speaks the same protocol.
     * @return Instance of a socket - based client.
     * @see com.damnvulnerableapp.networking.protocol.Protocol
     * @see ProtocolFactory
     * @see Client
     * @see Server
     */
    @Override
    public Client createClient() {

        NetworkClient client = new NetworkClient();
        client.setProtocol(this.getProtocolFactory().createClientProtocol());
        return client;
    }

    /**
     * Creates an instance of a socket - based server, i.e. it internally uses {@link java.net.ServerSocket}.
     * Also, using this {@link ProtocolFactory} set via
     * {@link CommunicationFactory#setProtocolFactory(ProtocolFactory)}, the server will be
     * given a {@link com.damnvulnerableapp.networking.protocol.Protocol} such that it can
     * communicate with connected clients that speaks the same protocol.
     * @return Instance of a socket - based server.
     * @see com.damnvulnerableapp.networking.protocol.Protocol
     * @see ProtocolFactory
     * @see Server
     * @see Client
     * */
    @Override
    public Server createServer() {

        NetworkServer server = new NetworkServer();
        server.setProtocol(this.getProtocolFactory().createServerProtocol());
        return server;
    }
}
