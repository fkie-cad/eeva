package com.damnvulnerableapp.test.networking.communication.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.messages.PlainMessageParser;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;
import com.damnvulnerableapp.networking.protocol.PlainServerProtocol;
import com.damnvulnerableapp.test.helpers.DynamicClientConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

// TODO: Write tests for changing configurations!
public class TestPlainNetworkClient {

    private static final int PORT = 8088;
    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = 2000;

    private PlainServerProtocol serverProtocol;
    private NetworkEndPoint serverSide;
    private ServerSocket server;
    private Thread acceptThread;

    private Client client;
    private NetworkConnectionInformation info;

    @Before
    public void Setup() throws Exception {

        final CommunicationFactory factory = new NetworkFactory();
        factory.setProtocolFactory(new PlainProtocolFactory());
        this.client = factory.createClient();
        this.info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        this.serverProtocol = new PlainServerProtocol();
        this.server = new ServerSocket(PORT);
        this.acceptThread = new Thread(() -> {
            try {
                while (true) {
                    serverSide = new NetworkEndPoint(server.accept());
                    serverProtocol.handshake(serverSide, null);
                }
            } catch (IOException | CommunicationException ignored) {}
        });
        this.acceptThread.start();
    }

    @After
    public void Close() throws Exception {
        if (this.serverSide != null)
            this.serverSide.disconnect();
        if (this.acceptThread.isAlive())
            this.acceptThread.interrupt();
        this.server.close();
    }

    @Test
    public void When_Connecting_Expect_Success() throws Exception {

        this.client.connect(this.info);

        while (!this.serverSide.isConnected());

        assertTrue(this.client.isConnected());
    }

    @Test
    public void When_ConnectingNoServer_Expect_ConnectionException() throws Exception {

        this.Close();

        assertThrows(ConnectionException.class, () -> this.client.connect(this.info));
    }

    @Test
    public void When_ConnectingReconnect_Expect_Success() throws Exception {

        this.client.connect(this.info);
        assertTrue(this.client.isConnected());

        this.client.disconnect();
        assertFalse(this.client.isConnected());

        this.client.connect(this.info);
        assertTrue(this.client.isConnected());
    }

    private static class ConnectListener implements CommunicationListener {

        private int amountConnects;

        public int amountConnects() {
            return this.amountConnects;
        }

        @Override
        public void onConnect(Client client) {
            this.amountConnects++;
        }

        @Override
        public void onDisconnect(Client client) {

        }

        @Override
        public void onSend(Client client, Message message) {

        }

        @Override
        public void onReceive(Client client, Message message) {

        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

        }
    }

    @Test
    public void When_ConnectedCheckingListener_Expect_Success() throws Exception {

        final ConnectListener listener = new ConnectListener();
        this.client.addCommunicationListener(listener);
        assertEquals(0, listener.amountConnects());
        this.client.connect(this.info);
        assertEquals(1, listener.amountConnects());
    }

    @Test
    public void When_Disconnecting_Expect_Success() throws Exception {

        this.client.connect(this.info);
        this.client.disconnect();
        assertFalse(this.client.isConnected());
    }

    @Test
    public void When_DisconnectingNotConnected_Expect_Success() {
        assertFalse(this.client.isConnected());
        this.client.disconnect();
        assertFalse(this.client.isConnected());
    }

    private static class DisconnectListener implements CommunicationListener {

        private int amountDisconnects;

        public int amountDisconnects() {
            return this.amountDisconnects;
        }

        @Override
        public void onConnect(Client client) {

        }

        @Override
        public void onDisconnect(Client client) {
            this.amountDisconnects++;
        }

        @Override
        public void onSend(Client client, Message message) {

        }

        @Override
        public void onReceive(Client client, Message message) {

        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

        }
    }

    @Test
    public void When_DisconnectingCheckingListener_Expect_Success() throws Exception {

        final DisconnectListener listener = new DisconnectListener();
        this.client.addCommunicationListener(listener);
        this.client.connect(this.info);
        assertEquals(0, listener.amountDisconnects());
        this.client.disconnect();
        assertEquals(1, listener.amountDisconnects());
    }

    @Test
    public void When_CheckingConnection_Expect_Success() throws Exception {

        assertFalse(this.client.isConnected());
        this.client.connect(this.info);
        assertTrue(this.client.isConnected());
        this.client.disconnect();
        assertFalse(this.client.isConnected());
    }

    @Test
    public void When_SendingSimpleMessage_Expect_Success() throws Exception {

        final PlainMessage message = new PlainMessage(Operation.SELECT, "Test123");

        this.client.connect(this.info);

        final Thread serverThread = new Thread(() -> {
            final PlainMessageParser parser = new PlainMessageParser();
            try {
                final PlainMessage received = (PlainMessage) parser.parseFromBytes(serverSide.receive());
                assertEquals(message.getOperation(), received.getOperation());
                assertEquals(message.getParameters().get(Parameter.CONTENT), received.getParameters().get(Parameter.CONTENT));
            } catch (MessageParserException | TimeoutException | ConnectionException ignored) {}
        });
        serverThread.start();

        this.client.send(message);

        serverThread.join();
    }

    @Test
    public void When_SendingDisconnected_Expect_ConnectionException() {

        assertThrows(ConnectionException.class, () -> this.client.send(new PlainMessage(Operation.SELECT, "Test123")));
    }

    private static class SendListener implements CommunicationListener {

        private int amountSends;

        public int amountSends() {
            return this.amountSends;
        }

        @Override
        public void onConnect(Client client) {

        }

        @Override
        public void onDisconnect(Client client) {
        }

        @Override
        public void onSend(Client client, Message message) {
            this.amountSends++;
        }

        @Override
        public void onReceive(Client client, Message message) {

        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

        }
    }

    @Test
    public void When_SendingCheckingListener_Expect_Success() throws Exception {

        final SendListener listener = new SendListener();
        this.client.addCommunicationListener(listener);
        this.client.connect(this.info);
        assertEquals(0, listener.amountSends());
        this.client.send(new PlainMessage(Operation.SELECT, "Test123"));
        assertEquals(1, listener.amountSends());
    }

    @Test
    public void When_SendingNull_Expect_Success() throws Exception {

        this.client.connect(this.info);
        this.client.send(null);
    }

    @Test
    public void When_Receiving_Expect_Success() throws Exception {

        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");

        this.client.connect(this.info);
        this.serverProtocol.send(this.serverSide, message);
        final PlainMessage received = (PlainMessage) this.client.receive();
        assertEquals(message.getOperation(), received.getOperation());
        assertArrayEquals(message.getParameters().get(Parameter.CONTENT), received.getParameters().get(Parameter.CONTENT));
    }

    @Test
    public void When_ReceivingDisconnected_Expect_ConnectionException() {

        assertThrows(ConnectionException.class, () -> this.client.receive());
    }

    @Test
    public void When_ReceivingNoMessage_Expect_TimeoutException() throws Exception {

        this.client.setConfiguration(new DynamicClientConfiguration(TIMEOUT));
        this.client.connect(this.info);
        assertThrows(TimeoutException.class, () -> this.client.receive());
    }

    private static class ReceiveListener implements CommunicationListener {

        private int amountReceives;

        public int amountReceives() {
            return this.amountReceives;
        }

        @Override
        public void onConnect(Client client) {

        }

        @Override
        public void onDisconnect(Client client) {
        }

        @Override
        public void onSend(Client client, Message message) {
        }

        @Override
        public void onReceive(Client client, Message message) {
            this.amountReceives++;
        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

        }
    }

    @Test
    public void When_ReceivingCheckingListener_Expect_Success() throws Exception {

        // Add listener and check connection status
        final ReceiveListener listener = new ReceiveListener();
        this.client.addCommunicationListener(listener);
        this.client.connect(this.info);
        assertEquals(0, listener.amountReceives());

        // Send message
        final PlainMessage message = new PlainMessage(Operation.SELECT, "test123");
        this.serverProtocol.send(this.serverSide, message);

        // Receive message and check connection status
        this.client.receive();
        assertEquals(1, listener.amountReceives());
    }

    private static class InvalidMessageListener implements CommunicationListener {

        private int amountInvalids;

        public int amountInvalids() {
            return this.amountInvalids;
        }

        @Override
        public void onConnect(Client client) {

        }

        @Override
        public void onDisconnect(Client client) {
        }

        @Override
        public void onSend(Client client, Message message) {
        }

        @Override
        public void onReceive(Client client, Message message) {

        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

            final PlainMessageParser parser = new PlainMessageParser();
            try {
                parser.parseFromBytes(message);
            } catch (MessageParserException e) {
                this.amountInvalids++;
            }
        }
    }

    @Test
    public void When_ReceivingInvalidMessageCheckingListener_Expect_Success() throws Exception {

        final InvalidMessageListener listener = new InvalidMessageListener();
        this.client.addCommunicationListener(listener);
        this.client.connect(this.info);

        assertEquals(0, listener.amountInvalids);

        // Send invalid message to client
        final byte[] message = "test123".getBytes();
        this.serverSide.send(message);

        // Receive message
        assertThrows(MessageParserException.class, () -> this.client.receive());

        assertEquals(1, listener.amountInvalids());
    }
}
