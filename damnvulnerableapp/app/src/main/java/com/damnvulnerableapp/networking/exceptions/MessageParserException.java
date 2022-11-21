package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.client.Client;

/**
 * Exception that will be thrown, if a {@link com.damnvulnerableapp.networking.messages.MessageParser}
 * fails to parse a message, i.e. the message deviates from a specified format. This exception is
 * used to trigger {@link com.damnvulnerableapp.networking.communication.client.CommunicationListener#onInvalidMessage(Client, byte[])},
 * which allows handling invalid messages that a parser fails to handle.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class MessageParserException extends CommunicationException {

    private final byte[] invalidMessage;

    public MessageParserException(String errorMessage, byte[] invalidMessage) {
        super("Parsing message failed: " + errorMessage);
        this.invalidMessage = invalidMessage;
    }

    public MessageParserException(String errorMessage, Throwable cause, byte[] invalidMessage) {
        super("Parsing message failed: " + invalidMessage, cause);
        this.invalidMessage = invalidMessage;
    }

    public final byte[] getInvalidMessage() {
        return this.invalidMessage;
    }
}
