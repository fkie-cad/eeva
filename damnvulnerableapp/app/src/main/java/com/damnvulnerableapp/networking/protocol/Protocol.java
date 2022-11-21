package com.damnvulnerableapp.networking.protocol;

import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.InvalidClientException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.MessageParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolStatus;

/**
 * Protocol that defines how to send and receive {@link Message}s using a low - level implementation
 * for communication, i.e. an {@link EndPoint}. It also defines how a connection is established and
 * shut down.
 *
 * This is an intermediate layer between a {@link com.damnvulnerableapp.networking.communication.client.Client}
 * and {@link EndPoint}, which abstracts from low - level byte arrays to {@link Message} - objects.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see com.damnvulnerableapp.networking.communication.client.Client
 * @see EndPoint
 * */
public abstract class Protocol {

    /**
     * Parser used to parse a {@link Message} from a byte array. It determines what {@link Message}
     * is used by this protocol.
     *
     * @see MessageParser
     * */
    private final MessageParser parser;

    /**
     * Constructs a protocol by assigning a {@link MessageParser}.
     *
     * @param parser Parser to use for parsing {@link Message} - objects from byte arrays.
     * */
    public Protocol(MessageParser parser) {
        this.parser = parser;
    }

    /**
     * Returns this parser. This is used in {@link Protocol#receive(EndPoint)} to get a {@link Message}
     * from a sequence of bytes.
     *
     * @return Parser used to parse {@link Message}.
     * */
    public MessageParser getParser() {
        return this.parser;
    }

    /**
     * Sends a message via an {@link EndPoint} using this protocol.
     *
     * @param ep {@link EndPoint} used to send the message. It should be a connected endpoint.
     * @param message Message to send via <code>ep</code>.
     * @throws ConnectionException If a connection error occurs while trying to send the message.
     * */
    public abstract void send(EndPoint ep, Message message) throws ConnectionException, MessageParserException, CommunicationException;

    /**
     * Tries to receive a {@link Message} via an {@link EndPoint}. This is a blocking call, as
     * {@link EndPoint#receive()} is a blocking call.
     *
     * @param ep {@link EndPoint} used to receive a message. It should be a connected endpoint.
     * @return Message parsed from received sequence of bytes.
     * @throws ConnectionException If a connection error occurs while trying to receive a message.
     * @throws MessageParserException If parsing a message from a sequence of bytes fails.
     * @throws TimeoutException If connection or reading times out (depends on {@link com.damnvulnerableapp.common.configuration.Configuration}).
     * */
    public abstract Message receive(EndPoint ep) throws ConnectionException, MessageParserException, TimeoutException, CommunicationException;

    // TODO: Pass arbitrary client information to handshake
    /**
     * Performs a handshake from the perspective of <code>ep</code>. Notice that there is no
     * deterministic sequence of send - and receive - calls for both, client - and server - side
     * {@link EndPoint}s, such that they successfully perform a handshake without a deadlock.
     * Therefore the client - side and server - side have to be distinguished.
     *
     * @param ep {@link EndPoint}, from whose perspective to perform the handshake.
     * @param type Identifier of the {@link com.damnvulnerableapp.networking.communication.client.Client}
     *             that wants to establish the connection. If it is not needed, it may be ignored.
     * @throws ConnectionException If a connection error occurs while performing the handshake.
     * @throws InvalidClientException If, from a server's perspective, a client with an incompatible
     *                                type tries to connect.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If connection or reading times out.
     * */
    public abstract void handshake(EndPoint ep, ClientType type) throws CommunicationException;

    /**
     * Shuts down an open connection by performing a sequence of sends and receives. This again is
     * from the perspective of <code>ep</code>.
     *
     * At best, the shut down is synchronized, i.e. before returning from this method, it should
     * wait for a specific message indicating that the communication partner realized that the
     * connection is going to be shut down. This could be achieved e.g. by waiting for a {@link PlainProtocolStatus#ACK}.
     * The reason for a synchronized shut down is that in some low - level implementations of
     * {@link EndPoint}, e.g. in {@link com.damnvulnerableapp.networking.communication.client.NetworkEndPoint},
     * closing the streams and socket will result in errors on the other side of communication.
     * Therefore a graceful shutdown is preferred.
     *
     * @param ep {@link EndPoint} used to perform the shutdown exchange. It will be closed as well,
     *                           e.g. in {@link Client#disconnect()} after this method returns.
     * @throws ConnectionException If a connection error occurs while trying to receive a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If reading times out.
     * */
    public abstract void shutdown(EndPoint ep) throws CommunicationException;
}
