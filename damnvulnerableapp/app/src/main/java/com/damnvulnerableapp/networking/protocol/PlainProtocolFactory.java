package com.damnvulnerableapp.networking.protocol;

import com.damnvulnerableapp.networking.communication.server.Server;

/**
 * Constructor of {@link PlainClientProtocol} and {@link PlainServerProtocol} that handle messages
 * of type {@link com.damnvulnerableapp.networking.messages.PlainMessage}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class PlainProtocolFactory implements ProtocolFactory {

    /**
     * Creates a {@link PlainClientProtocol} - object that is used by client - side {@link com.damnvulnerableapp.networking.communication.client.Client}s.
     *
     * @return Client - side protocol.
     * */
    @Override
    public ClientProtocol createClientProtocol() {
        return new PlainClientProtocol();
    }

    /**
     * Creates a {@link PlainServerProtocol} - object that is used by server - side {@link com.damnvulnerableapp.networking.communication.client.Client}s.
     * These are the result of {@link Server#accept()} and therefore need to speak a protocol that
     * is compatible with the client - side and does not result in deadlocks.
     *
     * @return Server - side protocol.
     * */
    @Override
    public ServerProtocol createServerProtocol() {
        return new PlainServerProtocol();
    }
}
