package com.damnvulnerableapp.networking.messages;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.networking.exceptions.MessageParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Parser that handles {@link PlainMessage}s. It is able to construct a {@link PlainMessage}
 * from a byte array.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see PlainMessage
 * */
public class PlainMessageParser implements MessageParser {

    /**
     * Parses a {@link PlainMessage} from a byte array. This method defines the structure of a
     * {@link PlainMessage}, i.e. every {@link PlainMessage} has to end in a {@link Parameter#CONTENT}
     * parameter and has to start with an {@link Operation}. Notice that the {@link Parameter#CONTENT}
     * has to be unique.
     *
     * @param message Array of bytes, from which to parse a {@link PlainMessage}.
     * @return {@link PlainMessage} that corresponds to the given byte array.
     * @throws MessageParserException If parsing fails. I.e. if <code>message</code> does not start
     *                                with an {@link Operation}, does not have any {@link Parameter}s
     *                                or does not end in a {@link Parameter#CONTENT} parameter. Also
     *                                passing null will trigger this exception or having more than
     *                                one {@link Parameter#CONTENT} parameters.
     * */
    @Override
    public @NonNull Message parseFromBytes(byte[] message) throws MessageParserException {

        if (message == null)
            throw new MessageParserException("Cannot parse null message.", null);

        // Message components
        Operation operation = null;
        int startIndex = 0;
        final HashMap<Parameter, byte[]> parameters = new HashMap<>();

        // Find variable
        boolean equal;

        // Check each operation
        for (Operation current : Operation.values()) {

            equal = true;

            // Get string of operation
            String op = current.toString();

            // Check lengths:
            if (op.length() > message.length)
                continue;

            // Check each byte of operation string
            for (int i = 0; i < op.length(); i++) {

                if (op.charAt(i) != message[i]) {

                    equal = false;
                    break;
                }
            }

            // Is operation in message? If yes, operation is unique!
            if (equal) {

                operation = current;
                startIndex = operation.toString().length() + 1;
                break;
            }
        }

        // If there is no operation, there is no (legal) message
        if (operation == null)
            throw new MessageParserException("No operation specified.", message);

        // Identify parameters. 'startIndex' points to first byte of parameters
        // 'first' and 'second' will each point to the current first / second parameter in message.
        int previous = 0;
        Parameter previousParam = null;
        for (int i = startIndex; i < message.length; i++) {

            for (Parameter parameter : Parameter.values()) {

                String param = parameter.toString();

                // Check parameter length
                if (param.length() > message.length - i)
                    continue;

                // Check value
                equal = true;

                for (int j = 0; j < param.length(); j++) {

                    if (param.charAt(j) != message[i + j]) {

                        equal = false;
                        break;
                    }
                }

                if (equal) {

                    // Check if parameter is surrounded by spaces
                    if ((i > 0 && message[i - 1] != ' ') || (i + param.length() < message.length && message[i + param.length()] != ' '))
                        throw new MessageParserException("Invalid separator(s). Only space separators allowed.", message);

                    // If 'previous' has not been set yet
                    if (previous != 0) {

                        // If this is a parameter that does not have any additional
                        // data, like e.g. DEBUG, just add empty byte array
                        byte[] data;
                        if (previous + previousParam.toString().length() + 1 > i - 1) {
                            data = new byte[0];
                        } else {
                            data = Arrays.copyOfRange(
                                    message,
                                    previous + previousParam.toString().length() + 1,
                                    i - 1
                            );
                        }

                        // Either way, knowing 'previous' and current matching allows
                        // inferring content of parameter pointed to by 'previous'.
                        parameters.put(previousParam, data);

                        // Set previous to second
                    }

                    previous = i;
                    previousParam = parameter;
                }
            }
        }

        // If there are NO parameters at all
        if (previous == 0)
            throw new MessageParserException("No parameters specified. There needs to be at least a"
                    + " " + Parameter.CONTENT + " parameter.", message);

        // Last parameter is ALWAYS 'CONTENT' -> check this
        if (!previousParam.toString().equals(Parameter.CONTENT.toString()))
            throw new MessageParserException("Last parameter must be " + Parameter.CONTENT
                    + ". It was " + previousParam, message);

        // Put final parameter into map
        parameters.put(
                previousParam,
                (previous + previousParam.toString().length() + 1 <= message.length) ?
                        Arrays.copyOfRange(
                        message,
                        previous + previousParam.toString().length() + 1,
                        message.length
                ) : new byte[0]
        );

        return new PlainMessage(operation, parameters);
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

        PlainMessage msg = (PlainMessage) message;
        String op = msg.getOperation().toString();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] value;
            stream.write(op.getBytes());

            // HashMap does not guarantee any order. Therefore, first add all other parameters
            // to the message.
            for (Parameter parameter : msg.getParameters().keySet()) {

                // Skip over trailing content
                if (parameter.equals(Parameter.CONTENT))
                    continue;

                value = msg.getParameters().get(parameter);
                stream.write(' ');
                stream.write(parameter.toString().getBytes());

                if (value != null && value.length > 0) {
                    stream.write(' ');
                    stream.write(value);
                }
            }

            // Now add mandatory content.
            byte[] content = msg.getParameters().get(Parameter.CONTENT);
            stream.write(' ');
            stream.write(Parameter.CONTENT.toString().getBytes());
            if (content != null && content.length > 0) {
                stream.write(' ');
                stream.write(content);
            }

        } catch (IOException e) {
            throw new MessageParserException("Failed to convert message to byte array.", message.toString().getBytes());
        }

        // Remove trailing space
        return stream.toByteArray();
    }

    /**
     * Determines whether a message is valid w.r.t. this parser that defines the structure and
     * contents of {@link PlainMessage} - objects.
     *
     * @param message Message to check.
     * @return <code>true</code>, if message is valid; <code>false</code> otherwise.
     * */
    @Override
    public boolean isValidMessage(Message message) throws MessageParserException {

        if (message == null)
            throw new MessageParserException("Cannot check null message", null);

        PlainMessage plain = (PlainMessage) message;

        if (plain.getParameters().get(Parameter.CONTENT) == null)
            return false;
        return true;
    }
}
