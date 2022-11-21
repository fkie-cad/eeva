package com.damnvulnerableapp.networking.protocol;

import com.damnvulnerableapp.networking.messages.MessageParser;

/**
 * Wrapper for all protocols that are to be performed on the client - side.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class ClientProtocol extends Protocol {

    /**
     * Construct protocol by fixing a {@link MessageParser} used to parse {@link com.damnvulnerableapp.networking.messages.Message}
     * objects from sequences of bytes.
     *
     * @param parser Parser to use for parsing messages.
     * */
    public ClientProtocol(MessageParser parser) {
        super(parser);
    }
}
