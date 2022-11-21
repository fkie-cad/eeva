package com.damnvulnerableapp.networking.messages;

import com.damnvulnerableapp.networking.communication.client.EndPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message that is the smallest unit of communication with remote clients that use
 * the {@link com.damnvulnerableapp.networking.protocol.PlainClientProtocol}.
 *
 * A message is of the form: <OPERATION> <PARAM 1> <PARAM 2> ... <PARAM N>
 * Notice that a parameter may contain additional data. Also, every "component" (i.e. word) in a
 * message is surrounded by spaces. The {@link Operation} is required as well as {@link Parameter#CONTENT}.
 * The {@link Parameter#CONTENT} has to be the last parameter in a {@link PlainMessage} and needs
 * to be unique (the uniqueness can only be broken if a message is directly sent to a communication
 * partner, i.e. if {@link com.damnvulnerableapp.networking.protocol.Protocol#send(EndPoint, Message)}
 * is NOT used).
 *
 * E.g. see the following message:
 *     SELECT CONTENT StackBasedBufferOverflow
 *     FORWARD CONTENT Hello there!
 * Wrong:
 *     SELECT CONTENT test123 CONTENT 321tset
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see Operation
 * @see Parameter
 * */
public class PlainMessage extends Message {

    /**
     * Operation that a client wants to perform. This is a required component of a message.
     * */
    private final Operation operation;

    /**
     * Map of {@link Parameter} - value pairs.
     * */
    private final HashMap<Parameter, byte[]> parameters;

    /**
     * Construct message from an {@link Operation} and a set of {@link Parameter} - objects.
     *
     * @param operation {@link Operation} that user wants to perform.
     * @param parameters Set of {@link Parameter} - objects that give further information on the
     *                   operation.
     * */
    public PlainMessage(Operation operation, HashMap<Parameter, byte[]> parameters) {
        this.operation = operation;

        this.parameters = parameters;
        if (this.parameters.get(Parameter.CONTENT) == null)
            this.parameters.put(Parameter.CONTENT, "".getBytes());
    }

    /**
     * Construct message from an {@link Operation} and the parameter {@link Parameter#CONTENT}.
     *
     * @param operation {@link Operation} that user wants to perform.
     * @param content Value of {@link Parameter#CONTENT}.
     * */
    public PlainMessage(Operation operation, byte[] content) {
        final HashMap<Parameter, byte[]> params = new HashMap<>();
        params.put(Parameter.CONTENT, (content != null) ? content : "".getBytes());

        this.operation = operation;
        this.parameters = params;
    }

    /**
     * Construct a message from an {@link Operation} and the parameter {@link Parameter#CONTENT}. The
     * parameter value is given by a string.
     *
     * @param operation {@link Operation} that user wants to perform.
     * @param content Value of {@link Parameter#CONTENT}.
     * */
    public PlainMessage(Operation operation, String content) {
        this(operation, (content != null) ? content.getBytes() : "".getBytes());
    }

    /**
     * Tries to convert a message into a readable string. Notice that a message may contain
     * binary data, which is not readable.
     *
     * @return String representation of a message.
     * */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.operation.toString());

        for (Map.Entry<Parameter, byte[]> entry : this.parameters.entrySet()) {
            builder.append(' ');
            builder.append(entry.getKey().toString());

            if (entry.getValue() != null && entry.getValue().length > 0) {
                builder.append(' ');
                builder.append(new String(entry.getValue()));
            }
        }

        return builder.toString();
    }

    /**
     * Gets {@link Operation} that user wants to perform.
     *
     * @return {@link Operation}
     * */
    public Operation getOperation() {
        return this.operation;
    }

    /**
     * Gets set of {@link Parameter} - value mappings that give further information on the
     * {@link Operation}.
     *
     * @return Map of {@link Parameter} - value pairs.
     * */
    public HashMap<Parameter, byte[]> getParameters() {
        return this.parameters;
    }
}
