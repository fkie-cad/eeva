package com.damnvulnerableapp.networking.communication.client;

import com.damnvulnerableapp.networking.messages.Message;

/**
 * Listener for communication events. These methods are invoked by the {@link Client}, with
 * whom this listener is registered (via {@link Client#addCommunicationListener(CommunicationListener)}).
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public interface CommunicationListener {

    /**
     * Handles the event that a client connected successfully, i.e. also {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}
     * was successful.
     *
     * @param client Client that triggered the event.
     * @see Client
     * */
    void onConnect(Client client);

    /**
     * Handles the event that a client wants to disconnect. This callback is invoked right before
     * any measures are taken to perform a disconnect. Therefore, finalizing messages can be
     * exchanged before shutting down the connection.
     *
     * Circumstances, under which this event is triggered vary from a user calling {@link Client#disconnect()}
     * to connection errors occurring in {@link Client#send(Message)} and {@link Client#receive()}.
     *
     * This callback is called at least once for a disconnect event. It may be called multiple times!
     *
     * @param client Client that triggered the event.
     * */
    void onDisconnect(Client client);

    /**
     * Handles the event that a client wants to send a message. This callback is called right before
     * the actual sending is performed (technically allowing to transform this into a chain of
     * responsibility pattern, if necessary). Do NOT call {@link Client#send(Message)} inside of this
     * method. Otherwise this may result in infinite recursion.
     *
     * @param client Client that wants to send a message.
     * @param message Message that will be sent.
     * */
    void onSend(Client client, Message message);

    /**
     * Handles the event that a client has received a message.
     *
     * This allows for constructing a request - response pattern, as a {@link com.damnvulnerableapp.networking.communication.server.Server}
     * may outsource any client into a thread that permanently calls {@link Client#receive()}. If
     * a message is received, this event is triggered, allowing a handler to send a response by
     * invoking {@link Client#send(Message)} inside of this callback.
     *
     * @param client Client that received a message.
     * @param message Message that has been received.
     * */
    void onReceive(Client client, Message message);

    /**
     * Handles the event that a client received an invalid message.
     *
     * It is possible that due to transmission errors etc. a message is not in a proper format, i.e.
     * a {@link com.damnvulnerableapp.networking.messages.MessageParser} throws an exception,
     * because the format of a message is unknown. Still the message might contain valuable
     * information that should not be missed.
     *
     * Notice that these kind of methods allow for easy pen - testing of this app, which is good
     * and bad at the same time. Therefore this callback should either be left empty when implementing
     * a listener or be carefully analyzed, as it may obtain arbitrary inputs from potentially
     * unknown sources.
     *
     * @param client Client that received an invalid message.
     * @param message Invalid message received by the client.
     * */
    void onInvalidMessage(Client client, byte[] message);
}
