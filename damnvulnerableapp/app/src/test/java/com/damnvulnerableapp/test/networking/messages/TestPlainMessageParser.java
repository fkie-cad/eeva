package com.damnvulnerableapp.test.networking.messages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.messages.PlainMessageParser;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class TestPlainMessageParser {

    @Test
    public void When_ParsingNull_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes(null));
    }

    @Test
    public void When_ParsingEmptyMessage_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes("".getBytes()));
    }

    @Test
    public void When_ParsingInvalidMessage_Expect_MessageParserException() {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final byte[] msg = ("#" + message.toString()).getBytes();
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes(msg));
    }

    @Test
    public void When_ParsingSimpleContentMessage_Expect_Success() throws Exception {
        final String content = "test123";
        final PlainMessage message = new PlainMessage(Operation.SELECT, content);
        final PlainMessageParser parser = new PlainMessageParser();
        final PlainMessage msg = (PlainMessage) parser.parseFromBytes(parser.toBytes(message));

        assertEquals(Operation.SELECT, msg.getOperation());
        assertEquals(content, new String(msg.getParameters().get(Parameter.CONTENT)));
    }

    @Test
    public void When_ParsingDoubleParameterMessage_Expect_Success() throws Exception {
        final String content = "test123";
        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, content.getBytes());
            put(Parameter.INFO, content.toUpperCase(Locale.ROOT).getBytes());
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();
        final PlainMessage msg = (PlainMessage) parser.parseFromBytes(parser.toBytes(message));

        assertEquals(Operation.SELECT, msg.getOperation());
        assertEquals(content, new String(msg.getParameters().get(Parameter.CONTENT)));
        assertEquals(content.toUpperCase(Locale.ROOT), new String(msg.getParameters().get(Parameter.INFO)));
    }

    @Test
    public void When_ParsingDoubleParameterOneNullMessage_Expect_Success() throws Exception {
        final String content = "test123";
        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, content.getBytes());
            put(Parameter.INFO, null);
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();
        final PlainMessage msg = (PlainMessage) parser.parseFromBytes(parser.toBytes(message));

        assertEquals(Operation.SELECT, msg.getOperation());
        assertEquals(content, new String(msg.getParameters().get(Parameter.CONTENT)));
        assertEquals(msg.getParameters().get(Parameter.INFO).length, 0);
    }

    @Test
    public void When_ParsingDoubleParameterNoContentMessage_Expect_Success() throws Exception {
        final String content = "test123";
        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, null);
            put(Parameter.INFO, content.getBytes());
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();
        final PlainMessage msg = (PlainMessage) parser.parseFromBytes(parser.toBytes(message));

        assertEquals(Operation.SELECT, msg.getOperation());
        assertEquals(content, new String(msg.getParameters().get(Parameter.INFO)));
        assertEquals(msg.getParameters().get(Parameter.CONTENT).length, 0);
    }

    @Test
    public void When_ParsingMessageNoParameters_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes(Operation.SELECT.toString().getBytes()));
    }

    @Test
    public void When_ParsingMessageNoContentParameter_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes((Operation.SELECT + " " + Parameter.INFO).getBytes()));
    }

    @Test
    public void When_ParsingOtherSeparator_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.parseFromBytes((Operation.SELECT + "#" + Parameter.CONTENT + "#" + "test123").getBytes()));
    }

    @Test
    public void When_ConvertingToBytesNull_Expect_MessageParserException() {
        final PlainMessageParser parser = new PlainMessageParser();
        assertThrows(MessageParserException.class, () -> parser.toBytes(null));
    }

    @Test
    public void When_ConvertingToBytesContent_Expect_Success() throws Exception {
        final PlainMessageParser parser = new PlainMessageParser();
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        assertArrayEquals((Operation.SELECT + " " + Parameter.CONTENT + " test123").getBytes(), parser.toBytes(message));
    }

    @Test
    public void When_ConvertingDoubleParameterMessage_Expect_Success() throws Exception {

        final String content = "test123";
        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, content.getBytes());
            put(Parameter.INFO, content.toUpperCase(Locale.ROOT).getBytes());
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();

        assertArrayEquals((Operation.SELECT + " " + Parameter.INFO + " " + content.toUpperCase(Locale.ROOT) + " " + Parameter.CONTENT + " " + content).getBytes(), parser.toBytes(message));
    }

    @Test
    public void When_ParsingDoubleParameterOneNull_Expect_Success() throws Exception {

        final String content = "test123";
        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, content.getBytes());
            put(Parameter.INFO, null);
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();

        assertArrayEquals((Operation.SELECT + " " + Parameter.INFO + " " + Parameter.CONTENT + " " + content).getBytes(), parser.toBytes(message));
    }

    @Test
    public void When_ParsingEmptyContent_Expect_Success() throws Exception {

        final HashMap<Parameter, byte[]> parameters = new HashMap<Parameter, byte[]>() {{
            put(Parameter.CONTENT, null);
        }};
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainMessageParser parser = new PlainMessageParser();

        assertArrayEquals((Operation.SELECT + " " + Parameter.CONTENT).getBytes(), parser.toBytes(message));
    }

    @Test
    public void When_CheckingMessage_Expect_Success() throws Exception {
        final PlainMessageParser parser = new PlainMessageParser();
        final String[] inputs = { null, "", "test123" };

        for (Operation op : Operation.values()) {
            for (String input : inputs) {

                assertTrue(parser.isValidMessage(new PlainMessage(op, input)));
            }
        }
    }
}
