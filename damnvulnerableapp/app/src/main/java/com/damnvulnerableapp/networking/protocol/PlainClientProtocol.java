package com.damnvulnerableapp.networking.protocol;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.messages.PlainMessageParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsule;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsuleParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolStatus;

/**
 * Client - side, plaintext - based protocol that uses {@link PlainMessage} and {@link PlainMessageParser}
 * to handle messaging. {@link PlainMessage} - objects are encapsulated with {@link PlainProtocolCapsule}
 * to allow passing meta - information alongside user data. This meta - information is used for
 * synchronizing and requesting shutdowns, and also for indicating that a message contains content
 * to be passed to the application.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class PlainClientProtocol extends ClientProtocol {

    /**
     * Constructs a client - side, plaintext - based protocol that fixes a {@link PlainProtocolCapsuleParser},
     * which again uses a {@link PlainMessageParser}, as its parser.
     * */
    public PlainClientProtocol() {
        super(new PlainProtocolCapsuleParser(new PlainMessageParser()));
    }

    /**
     * Sends a {@link Message}, which is not assumed to be encapsulated, via {@link EndPoint#send(byte[])}.
     * Before sending, the message is encapsulated with status code {@link PlainProtocolStatus#CONTENT}.
     *
     * @param ep {@link EndPoint} used to send the message.
     * @param message Message to send.
     * @throws ConnectionException If {@link EndPoint} is not connected or a connection error occurs
     *                             while sending a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws CommunicationException If any communication - related error occurs.
     * */
    @Override
    public void send(EndPoint ep, Message message) throws CommunicationException {

        if (!ep.isConnected())
            throw new ConnectionException("Endpoint is not connected.");

        // Send encapsulated package to communication partner.
        ep.send(this.getParser().toBytes(new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message)));
    }

    /**
     * Receives a {@link Message}, that is encapsulated, through {@link EndPoint#receive()}.
     *
     * If the status code is {@link PlainProtocolStatus#SHUTDOWN}, then this method will answer by
     * sending a {@link PlainProtocolStatus#ACK} that tells the communication partner that the
     * shutdown will be handled.
     *
     * @return A {@link Message} - object, if a message has been received; null otherwise.
     * @throws ConnectionException If a connection error occurs while reading a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If reading a message times out (depends on {@link com.damnvulnerableapp.common.configuration.ClientConfiguration}).
     * @throws CommunicationException If any communication - related error occurs.
     * */
    @Override
    public Message receive(EndPoint ep) throws CommunicationException {

        if (!ep.isConnected())
            throw new ConnectionException("Endpoint is not connected.");

        Message message = null;
        byte[] raw = ep.receive();
        if (raw != null) {

            PlainProtocolCapsule capsule = (PlainProtocolCapsule) this.getParser().parseFromBytes(raw);
            if (capsule.getStatus() == PlainProtocolStatus.SHUTDOWN) {

                // Handle shutdown
                ep.send(this.getParser().toBytes(new PlainProtocolCapsule(PlainProtocolStatus.ACK, null)));
            }
            message = capsule.getPayload();
        }

        return message;
    }

    // TODO: Generalize to arbitrary information about client.
    /**
     * Performs handshake by sending an empty encapsulated message with status code {@link PlainProtocolStatus#INIT}
     * and waiting for an empty message with status code {@link PlainProtocolStatus#ACK}. The initial
     * message contains the {@link ClientType} for identification. It might be ignored by the server.
     *
     * @param ep {@link EndPoint} used to send and receive messages.
     * @param type Purpose of the client.
     * @throws ConnectionException If a connection error occurs while sending or receiving a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If reading a message times out (depends on {@link com.damnvulnerableapp.common.configuration.ClientConfiguration}).
     * @throws CommunicationException If any communication - related error occurs.
     * */
    @Override
    public void handshake(EndPoint ep, ClientType type) throws CommunicationException {

        // 1. Send ClientType
        ep.send(this.getParser().toBytes(new PlainProtocolCapsule(PlainProtocolStatus.INIT, new PlainMessage(Operation.INIT, type.toString()))));

        // 2. Wait for ack
        this.waitForStatus(ep, PlainProtocolStatus.ACK);
    }

    /**
     * Shuts down the connection by sending an empty encapsulated message with status code {@link PlainProtocolStatus#SHUTDOWN}.
     * For synchronization purposes, this method will block until an empty message with status code
     * {@link PlainProtocolStatus#ACK} is received (notice the parallels to {@link PlainClientProtocol#receive(EndPoint)}).
     * 
     * @param ep {@link EndPoint} used for sending and receiving messages.
     * @throws ConnectionException If a connection error occurs while sending or receiving a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If reading a message times out (depends on {@link com.damnvulnerableapp.common.configuration.ClientConfiguration}).
     * @throws CommunicationException If any communication - related error occurs.
     * */
    @Override
    public void shutdown(EndPoint ep) throws CommunicationException {
        ep.send(this.getParser().toBytes(new PlainProtocolCapsule(PlainProtocolStatus.SHUTDOWN, null)));
        this.waitForStatus(ep, PlainProtocolStatus.ACK);
    }

    /**
     * Waits for a message with a specific {@link PlainProtocolStatus}.
     * 
     * There is a theoretical issue that occurs if a client tries to send multiple messages, where
     * the first message results in this method being called. Then consecutive messages will be
     * discarded. As this method is only called by {@link PlainClientProtocol#handshake(EndPoint, ClientType)}
     * and {@link PlainClientProtocol#shutdown(EndPoint)}, this should not be a practical problem.
     *
     * @param ep {@link EndPoint} used for sending and receiving messages.
     * @param status Status code to wait for.
     * @return Encapsulated message, whose status code is <code>status</code>.
     * @throws ConnectionException If a connection error occurs while sending or receiving a message.
     * @throws MessageParserException If parsing a message fails.
     * @throws TimeoutException If reading a message times out (depends on {@link com.damnvulnerableapp.common.configuration.ClientConfiguration}).
     * @throws CommunicationException If any communication - related error occurs.
     * */
    private PlainProtocolCapsule waitForStatus(EndPoint ep, PlainProtocolStatus status) throws CommunicationException {
        PlainProtocolCapsule capsule = null;
        while (capsule == null || capsule.getStatus() != status) {
            capsule = (PlainProtocolCapsule) this.getParser().parseFromBytes(ep.receive());
        }
        return capsule;
    }
}
