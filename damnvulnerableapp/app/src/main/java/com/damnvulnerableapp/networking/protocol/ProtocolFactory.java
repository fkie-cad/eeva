package com.damnvulnerableapp.networking.protocol;

import com.damnvulnerableapp.networking.communication.server.Server;

/**
 * Constructor for client - side and server - side protocols that are semantically linked and
 * compatible. This class is used in {@link com.damnvulnerableapp.networking.communication.CommunicationFactory}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public interface ProtocolFactory {

    /**
     * Constructs a client - side {@link ClientProtocol}. This should be compatible with the
     * {@link ServerProtocol} returned by {@link ProtocolFactory#createServerProtocol()}. It is
     * used by {@link com.damnvulnerableapp.networking.communication.client.Client} objects that
     * are not the result of an {@link Server#accept()} call.
     *
     * @return Client - side protocol implementation.
     * */
    ClientProtocol createClientProtocol();

    /**
     * Constructs a server - side {@link ServerProtocol}. This should be compatible with the
     * {@link ServerProtocol} returned by {@link ProtocolFactory#createClientProtocol()}. It is
     * used by {@link com.damnvulnerableapp.networking.communication.client.Client} objects that
     * are the result of an {@link Server#accept()} call.
     *
     * @return Server - side protocol implementation.
     * */
    ServerProtocol createServerProtocol();
}
