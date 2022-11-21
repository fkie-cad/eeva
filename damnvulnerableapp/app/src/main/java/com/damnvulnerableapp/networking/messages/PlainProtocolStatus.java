package com.damnvulnerableapp.networking.messages;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;

/**
 * Status code that tells e.g. {@link com.damnvulnerableapp.networking.protocol.PlainClientProtocol} and
 * {@link com.damnvulnerableapp.networking.protocol.PlainServerProtocol} what to do on specific
 * messages. E.g. this status code might indicate that an encapsulated message contains information
 * relevant for a protocol handshake.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public enum PlainProtocolStatus {

    /**
     * Indicates that an encapsulated message is part of a handshake performed by
     * {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}.
     * */
    INIT("INIT"),

    /**
     * Indicates that an encapsulated message is determined to be processed by the application, i.e.
     * it is user data that is independent of any communication - related procedures like handshakes.
     * */
    CONTENT("CONTENT"),

    /**
     * Indicates that an encapsulated message tries to confirm that another message arrived.
     */
    ACK("ACK"),

    /**
     * Indicates that one communication partner wants to shut down the connection. This could e.g.
     * be answered with an {@link PlainProtocolStatus#ACK} s.t. the shutdown can be synchronized.
     *
     * @see com.damnvulnerableapp.networking.protocol.PlainClientProtocol#shutdown(EndPoint)
     * */
    SHUTDOWN("SHUTDOWN");

    /**
     * Enum value.
     * */
    private final String status;

    /**
     * Set enum value.
     *
     * @param status Status code to associate with a message.
     * */
    PlainProtocolStatus(String status) {this.status = status;}

    /**
     * Converts enum value to a string.
     *
     * @return String value of the enum.
     * */
    @Override
    public String toString() { return this.status; }
}
