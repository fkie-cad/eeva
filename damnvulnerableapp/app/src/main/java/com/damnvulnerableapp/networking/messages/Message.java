package com.damnvulnerableapp.networking.messages;

import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;

/**
 * Wrapper that represents a message to be sent via {@link com.damnvulnerableapp.networking.protocol.Protocol#send(EndPoint, Message)}
 * and received via {@link com.damnvulnerableapp.networking.protocol.Protocol#receive(EndPoint)}.
 * To get a message from a sequence of raw bytes, {@link MessageParser#parseFromBytes(byte[])} is
 * used.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see MessageParser
 * @see com.damnvulnerableapp.networking.protocol.Protocol
 * */
public abstract class Message {

    /**
     * Converts this message to a string representation. This is useful for debugging - purposes.
     * It does NOT define the structure and content of a message! Therefore, this CANNOT be used
     * to implement e.g. {@link PlainMessageParser#toBytes(Message)}.
     *
     * @return String that represents a message.
     * */
    public abstract String toString();
}
