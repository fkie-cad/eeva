package com.damnvulnerableapp.networking.messages;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;

/**
 * Parser that determines how a stream of bytes can be interpreted as a {@link Message}. Therefore,
 * classes that implement this will define the structure and contents of a message that is sent
 * via {@link com.damnvulnerableapp.networking.protocol.Protocol#send(EndPoint, Message)} and
 * received via {@link com.damnvulnerableapp.networking.protocol.Protocol#receive(EndPoint)}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see Message
 * @see com.damnvulnerableapp.networking.protocol.Protocol
 * */
public interface MessageParser {

    /**
     * Parses a {@link Message} - object from a sequence of raw bytes. Notice that a subclass of
     * {@link Message} should always have its corresponding {@link MessageParser}.
     *
     * @param message Bytes, from which to construct a {@link Message} object.
     * @return Parsed message.
     * @throws MessageParserException If parsing a {@link Message} fails.
     * @see Message
     * @see PlainMessageParser
     * */
    @NonNull
    Message parseFromBytes(byte[] message) throws MessageParserException;

    /**
     * Converts a message to a byte array. This is used in {@link com.damnvulnerableapp.networking.protocol.Protocol#send(EndPoint, Message)}
     * as {@link EndPoint#send(byte[])} is only able to send byte arrays.
     *
     * @param message {@link Message} to convert to a byte array.
     * @return Array of bytes that represents a message.
     * @throws MessageParserException If given message is invalid w.r.t. this parser.
     * */
    byte[] toBytes(Message message) throws MessageParserException;

    /**
     * Determines whether a given message is valid as regards structure and content. Note that a
     * parser is responsible for defining the structure and allowed contents of a message in this
     * project, whereas a {@link Message} is just a data class.
     *
     * @param message {@link Message}, for which to determine whether it is valid or not.
     * @return <code>true</code>, if message is valid; <code>false</code> otherwise.
     * */
    boolean isValidMessage(Message message) throws MessageParserException;
}
