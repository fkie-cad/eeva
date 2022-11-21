package com.damnvulnerableapp.networking.communication.client;

import com.damnvulnerableapp.common.configuration.Configurable;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;

/**
 * Communication endpoint, i.e. one peer of a peer - to - peer communication. Its methods form
 * the basis of communication used in {@link Client} and {@link com.damnvulnerableapp.networking.protocol.Protocol}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see Configurable
 * */
public interface EndPoint {

    /**
     * Connects to specified location. This method has to ensure a stable connection after return.
     * It does not perform any handshakes, it just sets up the connection to the server.
     *
     * @param info Information on the connection that e.g. describes where to connect to.
     * @throws ConnectionException If an endpoint is already connected or some other connection
     *                             related error occurs.
     * @throws TimeoutException Connecting to a server may timeout, e.g. if the server is not
     *                          available or does not answer.
     * @throws CommunicationException If any communication - related error occurs.
     * */
    void connect(ConnectionInformation info) throws CommunicationException;

    /**
     * Disconnects this endpoint from an established connection. This method may be used to just
     * close resource, for which closing implies shutting down the connection like in {@link java.net.Socket}.
     * Notice that it should be possible to disconnect although there is no open connection.
     *
     * If an instance of a {@link Client} wants to disconnect, then it will internally call this
     * method.
     *
     * @see Client
     * */
    void disconnect();

    /**
     * Determines whether this endpoint is connected or not. This method is useful for instances of
     * {@link com.damnvulnerableapp.networking.protocol.Protocol} before sending or receiving
     * messages.
     *
     * @return <code>true</code>, if endpoint is connected; <code>false</code> otherwise.
     * @see com.damnvulnerableapp.networking.protocol.Protocol
     * */
    boolean isConnected();

    /**
     * Sends a raw byte array via a medium to the communication partner. It does not directly
     * verify whether the connection is open or not, but rather will figure this out through
     * exceptions.
     *
     * The procedure for sending a raw message can be as follows:
     * 1. Try to send a 4 - byte integer indicating the <code>length</code> of the actual message to send.
     * 2. Send the actual message.
     *
     * Notice that successfully sending a message does not imply that the communication partner
     * received the message. If this endpoint was closed right after send, then the message might
     * not be arriving at the receiver's end (Imagine a car(message) crossing a bridge(connection).
     * destroying the bridge while the car is still on it might also destroy the car).
     *
     * This method is the basis for {@link com.damnvulnerableapp.networking.protocol.Protocol#send(EndPoint, Message)}.
     *
     * @param message Array of bytes, i.e. a raw message, to send to the communication partner.
     * @throws ConnectionException If the connection is shut down during sending or has already
     *                             been shut down before sending.
     * @throws CommunicationException If any communication - related error occurs.
     * */
    void send(byte[] message) throws CommunicationException;

    /**
     * Receives a message from the communication partner. To do so, this method will block until
     * a message is available or an exception occurs, i.e. timeout or connection error.
     *
     * The procedure for receiving a raw message can be as follows:
     * 1. Try to read a 4 - byte integer indicating the <code>length</code> of the actual message to come.
     * 2. If the <code>length</code> is greater than 0, then wait for <code>length</code> - many bytes
     *    and read them into a buffer that is returned by this method.
     *
     * @return Raw message.
     * @throws ConnectionException If connection is shut down while or before waiting for a message.
     * @throws TimeoutException As receiving a message blocks until there is a message, this may
     *                          timeout. Therefore this is thrown on receive timeout.
     * @throws CommunicationException If any communication - related error occurs.
     * */
    byte[] receive() throws CommunicationException;

    /**
     * Sets the timeout for blocking operations, e.g. for {@link EndPoint#receive()}. E.g. for
     * {@link NetworkEndPoint}, this will call {@link java.net.Socket#setSoTimeout(int)}.
     *
     * @param timeout Timeout to use for blocking operations.
     * */
    void setTimeout(int timeout) throws CommunicationException;

    /**
     * Gets timeout used for blocking operations, e.g. for {@link EndPoint#receive()}.
     *
     * @return Timeout used for blocking operations.
     * */
    int getTimeout() throws CommunicationException;
}
