package com.damnvulnerableapp.test.networking.communication.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.damnvulnerableapp.common.configuration.ServerConfiguration;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.communication.server.NetworkBindInformation;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.exceptions.AcceptException;
import com.damnvulnerableapp.networking.exceptions.BindException;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.protocol.PlainClientProtocol;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;
import com.damnvulnerableapp.test.helpers.DynamicServerConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.ServerSocket;

// TODO: Check tests for resource leaks as regards bound server sockets!
public class TestPlainNetworkServer {

    private static final int PORT = 8081;
    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = 2000;

    private Server server;

    @Before
    public void Setup() {

        final NetworkFactory factory = new NetworkFactory();
        factory.setProtocolFactory(new PlainProtocolFactory());
        this.server = factory.createServer();
    }

    @After
    public void Close() throws Exception {

        this.server.close();
    }

    @Test
    public void When_BindingToLocalhost_Expect_Success() throws Exception {

        this.server.bind(new NetworkBindInformation(HOST, PORT));
        assertTrue(this.server.isBound());
    }

    @Test
    public void When_BindingToAlreadyBoundPort_Expect_BindException() throws Exception {

        new ServerSocket(PORT);
        assertThrows(BindException.class, () -> this.server.bind(new NetworkBindInformation(HOST, PORT)));
    }

    @Test
    public void When_BindingMultipleTimes_Expect_Success() throws Exception {
        this.server.bind(new NetworkBindInformation(HOST, PORT));
        this.server.bind(new NetworkBindInformation(HOST, PORT));
        this.server.bind(new NetworkBindInformation(HOST, PORT));
        this.server.bind(new NetworkBindInformation(HOST, PORT));
    }

    @Test
    public void When_CheckingIfBound_Expect_Success() throws Exception {

        assertFalse(this.server.isBound());
        this.server.bind(new NetworkBindInformation(HOST, PORT));
        assertTrue(this.server.isBound());
        this.server.close();
        assertFalse(this.server.isBound());
    }

    @Test
    public void When_AcceptingClient_Expect_Success() throws Exception {

        this.server.bind(new NetworkBindInformation(HOST, PORT));

        final Thread acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Client client = server.accept();
                    assertTrue(client.isConnected());
                    assertTrue(server.isBound());
                } catch (CommunicationException e) {
                    e.printStackTrace();
                }
            }
        });
        acceptThread.start();

        final NetworkEndPoint ep = new NetworkEndPoint();
        final PlainClientProtocol clientProtocol = new PlainClientProtocol();
        ep.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));
        clientProtocol.handshake(ep, ClientType.USER);
        assertTrue(ep.isConnected());

        acceptThread.join();
    }

    @Test
    public void When_AcceptingUnbound_Expect_AcceptException() {

        assertThrows(AcceptException.class, () -> this.server.accept());
    }

    @Test
    public void When_AcceptingNoClient_Expect_TimeoutException() throws Exception {

        final ServerConfiguration configuration = (ServerConfiguration) this.server.getConfiguration();
        final Field timeout = configuration.getClass().getDeclaredField("timeout");
        timeout.setAccessible(true);
        timeout.setInt(configuration, 1);   // 1ms timeout to fasten test

        this.server.bind(new NetworkBindInformation(HOST, PORT));
        assertThrows(TimeoutException.class, () -> this.server.accept());

        timeout.setInt(configuration, 0);
        timeout.setAccessible(false);
    }

    @Test
    public void When_AcceptingNoHandshake_Expect_TimeoutException() throws Exception {

        this.server.bind(new NetworkBindInformation(HOST, PORT));
        this.server.setConfiguration(new DynamicServerConfiguration(2000, 0, 50));

        final Thread acceptThread = new Thread(() -> {
            try {
                final EndPoint ep = new NetworkEndPoint();
                ep.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));
            } catch (CommunicationException e) {
                e.printStackTrace();
            }
        });
        acceptThread.start();

        assertThrows(TimeoutException.class, this.server::accept);

        acceptThread.join();
    }

    @Test
    public void When_AcceptingDisconnectWhileHandshake_Expect_TimeoutException() throws Exception {

        this.server.bind(new NetworkBindInformation(HOST, PORT));

        final Thread acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final EndPoint ep = new NetworkEndPoint();
                    ep.connect(new NetworkConnectionInformation(HOST, PORT, 0));
                    ep.disconnect();
                } catch (CommunicationException e) {
                    e.printStackTrace();
                }
            }
        });
        acceptThread.start();

        assertThrows(ConnectionException.class, () -> this.server.accept());

        acceptThread.join();
    }

    @Test
    public void When_ClosingWithoutBinding_Expect_Success() throws Exception {

        this.server.close();
        assertFalse(this.server.isBound());
    }
}
