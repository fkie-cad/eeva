package com.damnvulnerableapp.test.networking.messages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.messages.PlainMessageParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsule;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsuleParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolStatus;

import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;

public class TestPlainProtocolCapsuleParser {

    @Test
    public void When_ParsingNull_Expect_MessageParserException() throws Exception {
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes(null));
    }

    @Test
    public void When_ParsingEmptyMessage_Expect_MessageParserException() {
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes("".getBytes()));
    }

    @Test
    public void When_ParsingSimpleCapsule_Expect_Success() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final PlainProtocolCapsule caps = (PlainProtocolCapsule) parser.parseFromBytes(parser.toBytes(capsule));

        assertEquals(PlainProtocolStatus.CONTENT, caps.getStatus());
        assertEquals(message.toString(), caps.getPayload().toString());
    }

    @Test
    public void When_ParsingNoPayload_Expect_Success() throws Exception {
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, null);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final PlainProtocolCapsule caps = (PlainProtocolCapsule) parser.parseFromBytes(parser.toBytes(capsule));

        assertEquals(PlainProtocolStatus.CONTENT, caps.getStatus());
        assertNull(caps.getPayload());
    }

    @Test
    public void When_ParsingNoStatus_Expect_MessageParserException() {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainMessageParser plainParser = new PlainMessageParser();
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(plainParser);

        assertThrows(MessageParserException.class, () -> parser.parseFromBytes(plainParser.toBytes(message)));
    }

    @Test
    public void When_ParsingNoSeparatingSpace_Expect_MessageParserException() {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        assertThrows(MessageParserException.class, () -> parser.parseFromBytes((PlainProtocolStatus.CONTENT + message.toString()).getBytes()));
    }

    @Test
    public void When_ParsingOtherSeparator_Expect_MessageParserException() {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        assertThrows(MessageParserException.class, () -> parser.parseFromBytes((PlainProtocolStatus.CONTENT + "#" + message).getBytes()));
    }

    @Test
    public void When_ConvertingToBytesNull_Expect_MessageParserException() {
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        assertThrows(MessageParserException.class, () -> parser.toBytes(null));
    }

    @Test
    public void When_ConvertingToBytesEmpty_Expect_Success() throws Exception {
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        assertArrayEquals((PlainProtocolStatus.CONTENT + "").getBytes(), parser.toBytes(new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, null)));
    }

    @Test
    public void When_ConvertingToBytesContent_Expect_Success() throws Exception {

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        assertArrayEquals((PlainProtocolStatus.CONTENT + " " + Operation.SELECT + " " + Parameter.CONTENT + " test123").getBytes(), parser.toBytes(capsule));
    }

    @Test
    public void When_CheckingMessage_Expect_Success() throws Exception {
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final PlainMessage[] inputs = { null, new PlainMessage(Operation.SELECT, (byte[])null), new PlainMessage(Operation.SELECT, ""), new PlainMessage(Operation.SELECT, "test123") };

        for (PlainProtocolStatus status : PlainProtocolStatus.values()) {
            for (PlainMessage input : inputs) {

                assertTrue(parser.isValidMessage(new PlainProtocolCapsule(status, input)));
            }
        }
    }
}
