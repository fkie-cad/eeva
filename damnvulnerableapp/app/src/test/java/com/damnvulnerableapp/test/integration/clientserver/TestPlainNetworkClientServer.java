package com.damnvulnerableapp.test.integration.clientserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.communication.server.NetworkBindInformation;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TestPlainNetworkClientServer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 44444;
    private static final int TIMEOUT = 5000;
    private static final int MAX_CLIENTS = 50;

    private Server server;

    private NetworkFactory factory;

    @Before
    public void Setup() throws Exception {

        this.factory = new NetworkFactory();
        this.factory.setProtocolFactory(new PlainProtocolFactory());

        this.server = this.factory.createServer();
        this.server.bind(new NetworkBindInformation(HOST, PORT));
        assertTrue(this.server.isBound());
        this.server.startAsync();
    }

    @After
    public void Close() throws Exception {

        this.server.close();
    }

    @Test
    public void When_SingleClientConnects_Expect_Success() throws Exception {

        final Client client = this.factory.createClient();
        client.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));
        assertTrue(client.isConnected());
        client.disconnect();
    }

    /**
     * Flaky with almost negligible probability due to sleep
     * */
    @Test
    public void When_MultipleClientsConnect_Expect_Success() throws Exception {

        int i;
        final List<Client> clients = new LinkedList<>();
        for (i = 0; i < MAX_CLIENTS; i++)
            clients.add(this.factory.createClient());

        // Little stress test :)
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);
        for (i = 0; i < MAX_CLIENTS; i++)
            clients.get(i).connect(info);

        for (i = 0; i < MAX_CLIENTS; i++)
            assertTrue(clients.get(i).isConnected());

        // Its possible for this test to be faster than the last accept in server thread -> sleep
        Thread.sleep(1000);

        // Get method Server#getClients
        final Class<?> parent = this.server.getClass().getSuperclass();
        assertNotNull(parent);
        final Method getClients = parent.getDeclaredMethod("getClients");
        getClients.setAccessible(true);
        final Object result = getClients.invoke(this.server);
        assertTrue(result instanceof List);
        assertTrue(((List<?>)result).get(0) instanceof Client);
        final List<?> acceptedClients = (List<?>)result;

        // Check number of clients
        assertEquals(MAX_CLIENTS, acceptedClients.size());

        getClients.setAccessible(false);

        for (i = 0; i < MAX_CLIENTS; i++)
            clients.get(i).disconnect();
    }

    private int[] generateRandomSequence(int size) {

        int i, j;
        final Random randGen = new Random();

        int[] sequence = new int[2 * size];
        Arrays.fill(sequence, -1);

        int index, count;
        for (i = 0; i < 2 * size; i++) {

            while (sequence[i] == -1) {
                index = randGen.nextInt(size);

                // Count occurrences of current random index
                for (j = 0, count = 0; j < 2 * size; j++) {

                    if (sequence[j] == index)
                        count++;
                }

                // Every index may only occur twice, i.e. first for connect, second for disconnect.
                if (count < 2)
                    sequence[i] = index;
            }
        }

        return sequence;
    }

    /**
     * Might be flaky, if the message thread of the last client that disconnects is slower than
     * this method, i.e. if this method reaches the assertEquals before the last client can be
     * removed from the list of clients.
     * */
    @Test
    public void When_MultipleClientsConnectAndDisconnectRandomly_Expect_Success() throws Exception {

        final int[] sequence = this.generateRandomSequence(MAX_CLIENTS);
        final List<Client> clients = new ArrayList<>();
        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        for (int i = 0; i < MAX_CLIENTS; i++)
            clients.add(this.factory.createClient());

        // First occurrence of client index will make the client connect, the second will make the
        // client disconnect, as it is already connected.
        Client current;
        int numClients = 0;
        for (int s : sequence) {
            current = clients.get(s);
            if (!current.isConnected()) {
                current.connect(info);
                numClients++;
                assertTrue(current.isConnected());
            } else {
                current.disconnect();
                numClients--;
                assertFalse(current.isConnected());
            }
        }

        Thread.sleep(100);

        // Get method getClients
        final Class<?> parent = this.server.getClass().getSuperclass();
        assertNotNull(parent);
        final Method getClients = parent.getDeclaredMethod("getClients");
        getClients.setAccessible(true);
        final Object result = getClients.invoke(this.server);
        assertTrue(result instanceof List);
        final List<?> acceptedClients = (List<?>)result;
        assertEquals(numClients, acceptedClients.size());
        getClients.setAccessible(false);
    }

    private static class ServerListener implements CommunicationListener {

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

            try {
                client.send(message);
            } catch (CommunicationException ignored) {}
        }

        @Override
        public void onInvalidMessage(Client client, byte[] message) {

        }
    }

    @Test
    public void When_MultipleClientsRandomOrderEcho_Expect_Success() throws Exception {

        this.server.addCommunicationListener(new ServerListener());
        final int[] sequence = this.generateRandomSequence(MAX_CLIENTS);
        final List<Client> clients = new ArrayList<>();

        final ConnectionInformation info = new NetworkConnectionInformation(HOST, PORT, TIMEOUT);

        for (int i = 0; i < MAX_CLIENTS; i++)
            clients.add(this.factory.createClient());

        // First occurrence of client index will make the client connect, the second will make the
        // client disconnect, as it is already connected. On connect, send a message with unique
        // client id (index) and await echoed result.
        Client current;
        PlainMessage received;
        for (int s : sequence) {
            current = clients.get(s);
            if (!current.isConnected()) {
                current.connect(info);
                assertTrue(current.isConnected());

                final PlainMessage message = new PlainMessage( Operation.SELECT, "test" + s);
                current.send(message);
                received = (PlainMessage) current.receive();
                assertEquals(message.toString(), received.toString());

            } else {
                current.disconnect();
                assertFalse(current.isConnected());
            }
        }
    }

    // TODO: Implement unstable test cases, i.e. scenarios that trigger exceptions; random disconnects etc.


    private static class DisconnectHandler implements CommunicationListener {

        private int amountCalls;

        public int getAmountCalls() {
            return this.amountCalls;
        }

        @Override
        public void onConnect(Client client) {}

        @Override
        public void onDisconnect(Client client) {
            this.amountCalls++;
        }

        @Override
        public void onSend(Client client, Message message) {}

        @Override
        public void onReceive(Client client, Message message) {}

        @Override
        public void onInvalidMessage(Client client, byte[] message) {}
    }

    @Test
    public void When_ClientTimesOutCheckDisconnectEvent_Except_Success() throws Exception {

        final DisconnectHandler handler = new DisconnectHandler();
        this.server.addCommunicationListener(handler);
        this.server.setClientConfiguration(new ClientExitConfiguration());

        final Client client = this.factory.createClient();
        client.setConfiguration(new ClientExitConfiguration());
        client.connect(new NetworkConnectionInformation(HOST, PORT, TIMEOUT));
        try {
            client.receive();
        } catch (TimeoutException e) {
            Thread.sleep(new ClientExitConfiguration().getEndpointTimeout());
            assertEquals(1, handler.getAmountCalls());
            return;
        }
        fail();
    }
}
