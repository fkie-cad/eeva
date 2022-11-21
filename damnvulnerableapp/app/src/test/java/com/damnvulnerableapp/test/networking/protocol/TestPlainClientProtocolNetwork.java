package com.damnvulnerableapp.test.networking.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.messages.PlainMessageParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsule;
import com.damnvulnerableapp.networking.messages.PlainProtocolCapsuleParser;
import com.damnvulnerableapp.networking.messages.PlainProtocolStatus;
import com.damnvulnerableapp.networking.protocol.PlainClientProtocol;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * This class will provide tests for client - side protocols based on NetworkEndPoint
 * */
public class TestPlainClientProtocolNetwork {

    private static final int PORT = 8084;
    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = 2000;

    private ServerSocket server;
    private EndPoint serverSide;

    private EndPoint clientSide;

    @Before
    public void Setup() throws Exception {

        this.server = new ServerSocket(PORT);
        final Thread acceptThread = new Thread(() -> {
            try {
                serverSide = new NetworkEndPoint(server.accept());
            } catch (IOException | ConnectionException ignored) {}
        });
        acceptThread.start();

        this.clientSide = new NetworkEndPoint(new Socket());
        this.clientSide.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));

        acceptThread.join();
    }

    @After
    public void Close() throws IOException {
        this.clientSide.disconnect();
        if (this.serverSide.isConnected())
            this.serverSide.disconnect();
        this.server.close();
    }

    @Test
    public void When_SendingSimpleMessage_Expect_Success() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainClientProtocol proto = new PlainClientProtocol();
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        proto.send(this.clientSide, message);
        final PlainProtocolCapsule received = (PlainProtocolCapsule) parser.parseFromBytes(this.serverSide.receive());
        assertEquals(PlainProtocolStatus.CONTENT, received.getStatus());
        assertEquals(message.toString(), received.getPayload().toString());
    }

    /**
     * Currently it is not possible to craft {@link com.damnvulnerableapp.networking.messages.Message}
     * objects via their constructor that are invalid.
     * */
    @Test
    @Ignore
    public void When_SendingInvalidMessage_Expect_MessageParserException() {
        // Construct invalid message
        final HashMap<Parameter, byte[]> parameters = new HashMap<>();
        final PlainMessage message = new PlainMessage(Operation.SELECT, parameters);
        final PlainClientProtocol proto = new PlainClientProtocol();

        assertThrows(MessageParserException.class, () -> proto.send(this.clientSide, message));
    }

    @Test
    public void When_SendingDisconnected_Expect_ConnectionException() {
        this.clientSide.disconnect();

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainClientProtocol proto = new PlainClientProtocol();

        assertThrows(ConnectionException.class, () -> proto.send(this.clientSide, message));
    }

    // Flaky
    @Test
    public void When_SendingServerDisconnected_Expect_ConnectionException() {
        this.serverSide.disconnect();

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainClientProtocol proto = new PlainClientProtocol();

        assertThrows(ConnectionException.class, () -> proto.send(this.clientSide, message));
    }

    @Test
    public void When_SendingNull_Expect_Success() throws Exception {

        final PlainClientProtocol proto = new PlainClientProtocol();
        proto.send(this.clientSide, null);
    }

    // TODO: Determine whether this is a flaky test! serverSide.send(parser.toBytes(capsule)) caused exceptions because of closed output stream -> seems non-deterministic
    @Test
    public void When_ReceivingSimpleMessage_Expect_Success() throws Exception {

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.serverSide.send(parser.toBytes(capsule));

        final PlainClientProtocol proto = new PlainClientProtocol();
        final PlainMessage received = (PlainMessage) proto.receive(this.clientSide);

        assertEquals(message.getOperation(), received.getOperation());
        assertEquals(new String(message.getParameters().get(Parameter.CONTENT)), new String(received.getParameters().get(Parameter.CONTENT)));
        assertEquals(message.getParameters().size(), received.getParameters().size());
    }

    @Test
    public void When_ReceivingDisconnected_Expect_ConnectionException() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.serverSide.send(parser.toBytes(capsule));

        final PlainClientProtocol proto = new PlainClientProtocol();
        this.clientSide.disconnect();
        assertThrows(ConnectionException.class, () -> proto.receive(this.clientSide));
    }

    @Test
    public void When_ReceivingServerDisconnected_Expect_Success() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.serverSide.send(parser.toBytes(capsule));
        this.serverSide.disconnect();

        final PlainClientProtocol proto = new PlainClientProtocol();
        proto.receive(this.clientSide);
    }

    @Test
    public void When_ReceivingInvalidMessage_Expect_MessageParserException() throws Exception {

        this.serverSide.send((PlainProtocolStatus.CONTENT + "#").getBytes()); // <- invalid message

        final PlainClientProtocol proto = new PlainClientProtocol();
        assertThrows(MessageParserException.class, () -> proto.receive(this.clientSide));
    }

    @Test
    public void When_Handshake_Expect_Success() throws Exception {

        final ClientType type = ClientType.USER;
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread serverThread = new Thread(() -> {

            try {
                final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(serverSide.receive());
                assertEquals(type, ClientType.valueOf(new String(((PlainMessage)capsule.getPayload()).getParameters().get(Parameter.CONTENT))));
                serverSide.send(parser.toBytes(new PlainProtocolCapsule(PlainProtocolStatus.ACK, null)));
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        final PlainClientProtocol proto = new PlainClientProtocol();
        proto.handshake(this.clientSide, type);
    }

    @Test
    public void When_HandshakeWithoutAck_Expect_TimeoutException() throws Exception {

        final ClientType type = ClientType.USER;
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread serverThread = new Thread(() -> {

            try {
                final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(serverSide.receive());
                assertEquals(PlainProtocolStatus.INIT, capsule.getStatus());
                assertEquals(type, ClientType.valueOf(new String(((PlainMessage)capsule.getPayload()).getParameters().get(Parameter.CONTENT))));
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        final PlainClientProtocol proto = new PlainClientProtocol();
        this.clientSide.setTimeout(TIMEOUT);
        assertThrows(TimeoutException.class, () -> proto.handshake(this.clientSide, type));
    }

    @Test
    public void When_ShuttingDown_Expect_Success() throws Exception {

        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread serverThread = new Thread(() -> {

            try {

                final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(serverSide.receive());
                assertEquals(PlainProtocolStatus.SHUTDOWN, capsule.getStatus());
                serverSide.send(parser.toBytes(new PlainProtocolCapsule(PlainProtocolStatus.ACK, null)));
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        final PlainClientProtocol proto = new PlainClientProtocol();
        proto.shutdown(this.clientSide);
    }

    @Test
    public void When_ShuttingDownWithoutAck_Expect_TimeoutException() throws Exception {

        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread serverThread = new Thread(() -> {

            try {

                final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(serverSide.receive());
                assertEquals(PlainProtocolStatus.SHUTDOWN, capsule.getStatus());
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        final PlainClientProtocol proto = new PlainClientProtocol();
        this.clientSide.setTimeout(TIMEOUT);
        assertThrows(TimeoutException.class, () -> proto.shutdown(this.clientSide));
    }
}
