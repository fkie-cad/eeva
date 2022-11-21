package com.damnvulnerableapp.networking.exceptions;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;

/**
 * Exception that will be thrown, if an error occurs that is related to {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}.
 * E.g. it may be thrown in the case that an initial {@link com.damnvulnerableapp.networking.messages.PlainMessage}
 * does not specify {@link com.damnvulnerableapp.networking.messages.Operation#INIT}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class HandshakeException extends ProtocolException {

    public HandshakeException(String message) {
        super("Failed performing handshake: " + message);
    }

    public HandshakeException(String message, Throwable cause) {
        super("Failed performing handshake: " + message, cause);
    }
}
