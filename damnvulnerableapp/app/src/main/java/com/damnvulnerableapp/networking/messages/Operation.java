package com.damnvulnerableapp.networking.messages;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;

/**
 * Identifier for the purpose of a {@link PlainMessage}. It determines what operation is requested
 * by a client. For each operation, there is a set of parameters that may be used. Notice that
 * every {@link PlainMessage} MUST contain a {@link Parameter#CONTENT} parameter.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see PlainMessage
 * */
// TODO: Check if descriptions of SELECT, FORWARD etc. coincide with implementation!
// TODO: Add what parameters can be used for each operation.
public enum Operation {

    /**
     * Indicates that a client wants to set up a connection. Therefore this is part of
     * {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}.
     * */
    INIT("INIT"),

    /**
     * Selects a module, whose name is passed in {@link Parameter#CONTENT}. If there is already a
     * module selected, it must be EXIT first.
     * */
    SELECT("SELECT"),

    /**
     * Forwards an array of bytes to the running module. This functions as the input portion of I/O
     * for the running module.
     * */
    FORWARD("FORWARD"),

    /**
     * Tries to retrieve an array of bytes from the running module. This is the output portion of I/O
     * for the running module.
     * */
    FETCH("FETCH"),

    /**
     * Shuts down a running module, if any.
     * */
    EXIT("EXIT"),

    /**
     * Terminates the app.
     * */
    SHUTDOWN("SHUTDOWN"),

    /**
     * Indicates that a message sent by a user is invalid. Therefore this is sent as a response to
     * an invalid message.
     * */
    INVALID("INVALID"),

    /**
     * Indicates that a requested operation has been successfully completed. This is used to indicate
     * the response portion of the request - response model.
     * */
    SUCCESS("SUCCESS");

    /**
     * Enum value.
     * */
    private final String operation;

    /**
     * Set enum value.
     *
     * @param operation Indicates purpose of a {@link PlainMessage}.
     * */
    Operation(String operation) {this.operation = operation;}

    /**
     * Converts enum value to a string.
     *
     * @return String value of the enum.
     * */
    @Override
    public String toString() {return this.operation;}
}
