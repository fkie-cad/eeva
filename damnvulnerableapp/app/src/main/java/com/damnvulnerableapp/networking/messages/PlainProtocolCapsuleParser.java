package com.damnvulnerableapp.networking.messages;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Parser that constructs a {@link PlainProtocolCapsule} from e.g. a sequence of bytes returned by
 * {@link EndPoint#receive()}. It determines the structure of {@link PlainProtocolCapsule}, which
 * is limited to preceding a message with ONE {@link PlainProtocolStatus}. Notice that the encapsulated
 * message might be null.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see PlainProtocolCapsule
 * */
public class PlainProtocolCapsuleParser implements MessageParser {

    /**
     * Parser used to parse this encapsulated message.
     * */
    private final MessageParser payloadParser;

    /**
     * Constructs this parser for parsing {@link PlainProtocolCapsule}s. It also sets the corresponding
     * {@link MessageParser} used to parse the encapsulated message.
     * */
    public PlainProtocolCapsuleParser(MessageParser payloadParser) {
        this.payloadParser = payloadParser;
    }

    /**
     * Parses an encapsulated message from a sequence of bytes.
     *
     * @param message Array of bytes, from which to parse the encapsulated message.
     * @return Message capsule. Notice that the encapsulated message might be null, depending on the
     *         use - case.
     * @throws MessageParserException If parsing the {@link PlainProtocolCapsule} fails.
     * */
    @Override
    public @NonNull Message parseFromBytes(byte[] message) throws MessageParserException {

        if (message == null)
            throw new MessageParserException("Cannot parse null message.", null);

        // Find variable
        boolean equal;

        // Check each status code
        PlainProtocolStatus status = null;
        int startIndex = -1;

        for (PlainProtocolStatus current : PlainProtocolStatus.values()) {

            equal = true;

            // Get string of operation
            String s = current.toString();

            // Check lengths:
            if (s.length() > message.length)
                continue;

            // Check each byte of operation string
            for (int i = 0; i < s.length(); i++) {

                if (s.charAt(i) != message[i]) {

                    equal = false;
                    break;
                }
            }

            // Is operation in message? If yes, operation is unique!
            if (equal) {

                status = current;
                startIndex = status.toString().length() + 1;
                break;
            }
        }

        if (status == null)
            throw new MessageParserException("Protocol capsule is missing a status code.", message);

        if (startIndex - 1 < message.length && message[startIndex - 1] != ' ')
            throw new MessageParserException("Invalid separator. Only space separators allowed.", message);

        return new PlainProtocolCapsule(
                status,
                (startIndex <= message.length) ? this.payloadParser.parseFromBytes(
                        Arrays.copyOfRange(
                                message,
                                startIndex,
                                message.length
                        )
                ) : null
        );
    }

    /**
     * Converts a given message into a sequence of bytes. This can be used to prepare a message
     * s.t. it can be sent via {@link com.damnvulnerableapp.networking.communication.client.EndPoint#send(byte[])}.
     *
     * @param message Message to convert to a byte array.
     * @return Converted message.
     * @throws MessageParserException If message is not valid or could not be converted into a byte
     *                                array.
     * */
    @Override
    public byte[] toBytes(Message message) throws MessageParserException {

        if (!this.isValidMessage(message))
            throw new MessageParserException("Invalid message format.", message.toString().getBytes());

        PlainProtocolCapsule capsule = (PlainProtocolCapsule) message;
        if (capsule.getPayload() == null)
            return capsule.getStatus().toString().getBytes();

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(capsule.getStatus().toString().getBytes());
            stream.write(' ');
            stream.write(this.payloadParser.toBytes(capsule.getPayload()));
        } catch (IOException e) {
            throw new MessageParserException("Failed to convert message to byte array.", message.toString().getBytes());
        }

        return stream.toByteArray();
    }

    /**
     * Determines whether a message is valid w.r.t. this parser that defines the structure and
     * contents of {@link PlainProtocolCapsule} - objects.
     *
     * @param message Message to check.
     * @return <code>true</code>, if message is valid; <code>false</code> otherwise.
     * */
    @Override
    public boolean isValidMessage(Message message) throws MessageParserException {

        if (message == null)
            throw new MessageParserException("Cannot check null message.", null);

        // Valid, if payload is valid.
        final PlainProtocolCapsule msg = (PlainProtocolCapsule) message;
        if (msg.getPayload() != null)
            return this.payloadParser.isValidMessage(msg.getPayload());
        return true;
    }
}
