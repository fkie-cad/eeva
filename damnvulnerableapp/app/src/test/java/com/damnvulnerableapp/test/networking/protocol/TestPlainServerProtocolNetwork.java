package com.damnvulnerableapp.test.networking.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import com.damnvulnerableapp.networking.protocol.PlainServerProtocol;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TestPlainServerProtocolNetwork {

    private static final int PORT = 8083;
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
        final PlainServerProtocol proto = new PlainServerProtocol();
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        proto.send(this.serverSide, message);
        final PlainProtocolCapsule received = (PlainProtocolCapsule) parser.parseFromBytes(this.clientSide.receive());
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
        final PlainServerProtocol proto = new PlainServerProtocol();

        assertThrows(MessageParserException.class, () -> proto.send(this.serverSide, message));
    }

    @Test
    public void When_SendingDisconnected_Expect_ConnectionException() {
        this.serverSide.disconnect();

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainServerProtocol proto = new PlainServerProtocol();

        assertThrows(ConnectionException.class, () -> proto.send(this.serverSide, message));
    }

    @Test
    public void When_SendingClientDisconnected_Expect_ConnectionException() {
        this.clientSide.disconnect();

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainServerProtocol proto = new PlainServerProtocol();

        assertThrows(ConnectionException.class, () -> proto.send(this.serverSide, message));
    }

    // TODO: Determine whether this is a flaky test! serverSide.send(parser.toBytes(capsule)) caused exceptions because of closed output stream -> seems non-deterministic
    @Test
    public void When_ReceivingSimpleMessage_Expect_Success() throws Exception {

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.clientSide.send(parser.toBytes(capsule));

        final PlainServerProtocol proto = new PlainServerProtocol();
        final PlainMessage received = (PlainMessage) proto.receive(this.serverSide);

        assertEquals(message.getOperation(), received.getOperation());
        assertEquals(new String(message.getParameters().get(Parameter.CONTENT)), new String(received.getParameters().get(Parameter.CONTENT)));
        assertEquals(message.getParameters().size(), received.getParameters().size());
    }

    @Test
    public void When_ReceivingDisconnected_Expect_ConnectionException() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.clientSide.send(parser.toBytes(capsule));

        final PlainServerProtocol proto = new PlainServerProtocol();
        this.serverSide.disconnect();
        assertThrows(ConnectionException.class, () -> proto.receive(this.serverSide));
    }

    @Test
    public void When_ReceivingClientDisconnected_Expect_Success() throws Exception {
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        final PlainProtocolCapsule capsule = new PlainProtocolCapsule(PlainProtocolStatus.CONTENT, message);
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());

        this.clientSide.send(parser.toBytes(capsule));
        this.clientSide.disconnect();

        final PlainServerProtocol proto = new PlainServerProtocol();
        proto.receive(this.serverSide);
    }

    @Test
    public void When_ReceivingInvalidMessage_Expect_MessageParserException() throws Exception {

        this.clientSide.send((PlainProtocolStatus.CONTENT + "#").getBytes()); // <- invalid message

        final PlainServerProtocol proto = new PlainServerProtocol();
        assertThrows(MessageParserException.class, () -> proto.receive(this.serverSide));
    }

    @Test
    public void When_Handshake_Expect_Success() throws Exception {

        final ClientType type = ClientType.USER;
        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    clientSide.send(parser.toBytes(new PlainProtocolCapsule(PlainProtocolStatus.INIT, new PlainMessage(Operation.INIT, type.toString()))));

                    final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(clientSide.receive());
                    assertEquals(PlainProtocolStatus.ACK, capsule.getStatus());
                    assertNull(capsule.getPayload());
                } catch (CommunicationException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();

        final PlainServerProtocol proto = new PlainServerProtocol();
        proto.handshake(this.serverSide, type);

        serverThread.join();
    }

    @Test
    public void When_HandshakeNoInit_Expect_TimeoutException() throws Exception {

        final PlainServerProtocol proto = new PlainServerProtocol();
        this.serverSide.setTimeout(TIMEOUT);
        assertThrows(TimeoutException.class, () -> proto.handshake(this.serverSide, ClientType.MANAGER));
    }

    @Test
    public void When_ShuttingDown_Expect_Success() throws Exception {

        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread clientThread = new Thread(() -> {

            try {

                final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(clientSide.receive());
                assertEquals(PlainProtocolStatus.SHUTDOWN, capsule.getStatus());
                clientSide.send(parser.toBytes(new PlainProtocolCapsule(PlainProtocolStatus.ACK, null)));
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        clientThread.start();

        final PlainServerProtocol proto = new PlainServerProtocol();
        proto.shutdown(this.serverSide);

        clientThread.join();
    }

    @Test
    public void When_ShuttingDownWithoutAck_Expect_TimeoutException() throws Exception {

        final PlainProtocolCapsuleParser parser = new PlainProtocolCapsuleParser(new PlainMessageParser());
        final Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    final PlainProtocolCapsule capsule = (PlainProtocolCapsule) parser.parseFromBytes(clientSide.receive());
                    assertEquals(PlainProtocolStatus.SHUTDOWN, capsule.getStatus());
                } catch (CommunicationException e) {
                    e.printStackTrace();
                }
            }
        });
        clientThread.start();

        final PlainClientProtocol proto = new PlainClientProtocol();
        this.serverSide.setTimeout(TIMEOUT);
        assertThrows(TimeoutException.class, () -> proto.shutdown(this.serverSide));
    }
}
