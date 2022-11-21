package com.damnvulnerableapp.networking.messages;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;

/**
 * Wrapper for arbitrary {@link Message}s. It adds control information to a {@link Message} by
 * preceding a message with a {@link PlainProtocolStatus}. Notice that this class may encapsulate
 * another capsule, i.e. it is arbitrarily recursive. Mind the tradeoff between message size and
 * utility when encapsulating another capsule.
 *
 * Also, this class only adds ONE {@link PlainProtocolStatus} to a {@link Message}. For other protocols,
 * it might be useful to create a new capsule that allows for a more dynamic way of handling
 * additional parameters.
 *
 * The overall structure is like the following:
 *     <STATUS><MESSAGE>
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see PlainProtocolCapsuleParser
 * */
public class PlainProtocolCapsule extends Message {

    /**
     * Additional information that will precede any encapsulated message.
     * */
    private final PlainProtocolStatus status;

    /**
     * Message that is encapsulated.
     * */
    private final Message payload;

    /**
     * Constructs capsule from a status code and a message.
     *
     * @param status Additional information that precede <code>payload</code>.
     * @param payload Message to encapsulate.
     * */
    public PlainProtocolCapsule(PlainProtocolStatus status, Message payload) {
        this.status = status;
        this.payload = payload;
    }

    /**
     * Converts encapsulated message to a {@link String}.
     *
     * @return If {@link PlainProtocolCapsule#payload} is null, then it will only return the status
     *         code; otherwise status code and {@link Message#toString()} are returned, separated
     *         by a space.
     * */
    @Override
    public String toString() {
        if (this.payload == null)
            return this.status.toString();
        return this.status.toString()  + " " + this.payload;
    }

    /**
     * Returns {@link PlainProtocolStatus} associated with this encapsulated message. This is used
     * in {@link com.damnvulnerableapp.networking.protocol.PlainClientProtocol#handshake(EndPoint, ClientType)}.
     *
     * @return Status code associated with the encapsulated message.
     * */
    public PlainProtocolStatus getStatus() {
        return this.status;
    }

    /**
     * Returns encapsulated {@link Message}. This may be null, depending on where this capsule is
     * used.
     *
     * @return Encapsulated message.
     * */
    public Message getPayload() {
        return this.payload;
    }
}
