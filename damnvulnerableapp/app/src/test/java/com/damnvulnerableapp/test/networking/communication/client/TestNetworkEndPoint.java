package com.damnvulnerableapp.test.networking.communication.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestNetworkEndPoint {

    private static final int PORT = 8082;
    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = 2000;

    private ServerSocket server;

    /**
     * Creates a server in test cases where a server is needed.
     * */
    @Before
    public void SetupServer() throws IOException {
        this.server = new ServerSocket(PORT);
    }

    /**
     * Closes a server.
     * */
    @After
    public void CloseServer() {
        try {
            if (this.server != null)
                this.server.close();
        } catch (IOException ignored) {}
    }

    @Test
    public void When_ConnectingToLocal_Expect_Success() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        assertTrue(ep.isConnected());

    }

    @Test
    public void When_ConnectingToLocalNoServer_Expect_ConnectionException() throws Exception {
        this.CloseServer();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        assertThrows(ConnectionException.class, () -> ep.connect(info));
    }

    /**
     * Flaky test, i.e. passes with very high probability, but might fail, if endpoint is faster than
     * 1ms to connect.
     * */
    // TODO: Figure out a way to make this test stable -> how to manually trigger connection timeout
    @Test
    @Ignore("Too flaky...")
    public void When_ConnectingToLocalTimeout_Expect_TimeoutException() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, 1);

        assertThrows(TimeoutException.class, () -> ep.connect(info));
    }

    @Test
    public void When_ReconnectingToLocal_Expect_ConnectionException() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        assertThrows(ConnectionException.class, () -> ep.connect(info));
    }

    /**
     * Check that reusing a socket results in connection exception
     * */
    @Test
    public void When_ReconnectingAfterCloseToLocal_Expect_ConnectionException() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.disconnect();
        assertThrows(ConnectionException.class, () -> ep.connect(info));
    }

    @Test
    public void When_CheckingConnectionIfNotConnected_Expect_Success() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        assertFalse(ep.isConnected());
    }

    @Test
    public void When_CheckingConnectionAfterDisconnect_Expect_Success() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.disconnect();
        assertFalse(ep.isConnected());
    }

    @Test
    public void When_SendingNullToLocal_Expect_Success() throws Exception {
        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                final DataInputStream input = new DataInputStream(other.getInputStream());
                final int numBytes = input.readInt();
                input.close();
                other.close();

                assertEquals(numBytes, 0);
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.send(null);
        ep.disconnect();

        th.join();
    }

    @Test
    public void When_SendingEmptyMessageToLocal_Expect_Success() throws Exception {
        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                final DataInputStream input = new DataInputStream(other.getInputStream());
                final int numBytes = input.readInt();
                input.close();
                other.close();

                assertEquals(numBytes, 0);
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.send("".getBytes());
        ep.disconnect();

        th.join();
    }

    @Test
    public void When_SendingMessageToLocal_Expect_Success() throws Exception {
        final String message = "test123";

        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                final DataInputStream input = new DataInputStream(other.getInputStream());
                final int numBytes = input.readInt();
                final byte[] bytes = new byte[numBytes];
                input.readFully(bytes, 0, numBytes);
                input.close();
                other.close();

                assertEquals(numBytes, message.length());
                assertEquals(message, new String(bytes));
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.send(message.getBytes());
        ep.disconnect();

        th.join();
    }

    /**
     * Same as not calling {@link EndPoint#connect(ConnectionInformation)} at all.
     * */
    @Test
    public void When_SendingMessageDisconnected_Expect_ConnectionException() throws Exception {
        final String message = "test123";

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.disconnect();
        assertThrows(ConnectionException.class, () -> ep.send(message.getBytes()));
    }

    @Test
    public void When_SendingMessageServerDisconnect_Expect_ConnectionException() throws Exception {
        final String message = "test123";

        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                other.close();
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        th.join();

        assertThrows(ConnectionException.class, () -> ep.send(message.getBytes()));
    }

    @Test
    public void When_ReceivingEmptyFromLocal_Expect_Success() throws Exception {
        final String message = "";

        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                final DataOutputStream output = new DataOutputStream(other.getOutputStream());
                output.writeInt(message.length());
                output.write(message.getBytes(), 0, message.length());
                output.flush();
                output.close();
                other.close();
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);

        byte[] received = ep.receive();
        assertEquals(received.length, message.length());
        assertEquals(new String(received), message);

        th.join();
        ep.disconnect();
    }

    @Test
    public void When_ReceivingMessageFromLocal_Expect_Success() throws Exception {
        final String message = "test123";

        final Thread th = new Thread(() -> {
            try {
                final Socket other =  server.accept();
                final DataOutputStream output = new DataOutputStream(other.getOutputStream());
                output.writeInt(message.length());
                output.write(message.getBytes(), 0, message.length());
                output.flush();
                output.close();
                other.close();
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);

        byte[] received = ep.receive();
        assertEquals(received.length, message.length());
        assertEquals(new String(received), message);

        th.join();
        ep.disconnect();
    }

    @Test
    public void When_ReceivingMessageDisconnected_Expect_ConnectionException() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);
        ep.disconnect();
        assertThrows(ConnectionException.class, ep::receive);
    }

    @Test
    public void When_ReceivingMessageServerDisconnect_Expect_Success() throws Exception {
        final Thread th = new Thread(() -> {
            try {
                final Socket other = server.accept();
                other.close();
            } catch (IOException ignored) {}
        });
        th.start();

        final EndPoint ep = new NetworkEndPoint();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        ep.connect(info);

        assertThrows(ConnectionException.class, ep::receive);

        th.join();
        ep.disconnect();
    }

    @Test
    public void When_ReceivingMessageTimeout_Expect_Success() throws Exception {
        final EndPoint ep = new NetworkEndPoint();
        ep.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));
        ep.setTimeout(TIMEOUT);

        assertThrows(TimeoutException.class, ep::receive);

        ep.disconnect();
    }

    /*
     * Future tests:
     * - Construct NetworkEndPoint with initialized socket and I/O streams -> parallel streams?
     */
}
