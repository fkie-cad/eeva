package com.damnvulnerableapp.networking.communication.server;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.common.configuration.Configuration;
import com.damnvulnerableapp.common.configuration.ServerConfiguration;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;
import com.damnvulnerableapp.networking.communication.client.EndPoint;
import com.damnvulnerableapp.networking.communication.client.NetworkClient;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.exceptions.AcceptException;
import com.damnvulnerableapp.networking.exceptions.BindException;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.CreationException;
import com.damnvulnerableapp.networking.exceptions.InternalSocketException;
import com.damnvulnerableapp.networking.exceptions.InvalidClientException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

/**
 * Socket - based implementation of {@link Server} that uses a {@link ServerSocket} to handle
 * incoming connections. Accepted client are then wrapped by {@link NetworkClient}, which is
 * compatible with this server.
 *
 * {@link NetworkClient} and {@link NetworkServer} should use the same protocol.
 *
 * This class should NEVER be instantiated by calling its constructor. Rather call
 * {@link NetworkFactory#createServer()} to obtain an instance of this class.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkServer extends Server {

    /**
     * Socket used to handle incoming client connections.
     * */
    private ServerSocket socket;

    /**
     * List of message - loop threads that repeatedly call {@link Client#receive()}, which results
     * in a request - response model, because a server - side client can only respond to a request
     * through {@link CommunicationListener#onReceive(Client, Message)}.
     * */
    private final List<Thread> messageThreads;

    /**
     * Construct server.
     * */
    public NetworkServer() {
        this.messageThreads = new LinkedList<>();
    }

    /**
     * Binds socket - based server to specified host and port. It also specifies the receive - timeout
     * according to the configuration of this server.
     *
     * @param info Instance of {@link NetworkBindInformation} that contains information on where
     *             to bind the server socket to.
     * @throws BindException If calling {@link ServerSocket#bind(SocketAddress)} fails.
     * @throws CreationException If creating the server socket fails.
     * @throws InternalSocketException If setting a timeout triggers an internal protocol error (TCP).
     * @see NetworkBindInformation
     * @see ServerSocket#bind(SocketAddress)
     * */
    @Override
    public void bind(BindInformation info) throws BindException, CreationException, InternalSocketException {

        // If socket is bound already, i.e. it has been used before, then interpret this
        // call as re-bind.
        if (this.isBound()) {
            this.close();
            this.socket = null;
        }

        NetworkBindInformation target = (NetworkBindInformation) info;
        try {

            // If the server socket has not been created yet
            if (this.socket == null)
                this.socket = new ServerSocket();

        } catch (IOException e) {
            throw new CreationException("Failed to create server socket.", e);
        }

        try {
            // Bind socket
            this.socket.bind(new InetSocketAddress(target.getHost(), target.getPort()));
        } catch (IOException e) {
            throw new BindException("Failed to bind server socket to " + target.getHost() + ":" + target.getPort(), e);
        }

        try {
            // Try to set timeout
            this.socket.setSoTimeout(((ServerConfiguration)this.getConfiguration()).getTimeout());
        } catch (SocketException e) {
            // This is specific to the underlying protocol, i.e. in this case: TCP
            // This might also be quite rare
            throw new InternalSocketException("Trying to set timeout on server socket failed.", e);
        }
    }

    /**
     * Checks whether the server is bound, i.e. whether there has been a successful call to
     * {@link Server#bind(BindInformation)}. Notice that if this {@link ServerSocket} is bound, then
     * it cannot be closed. On the other hand, if this {@link ServerSocket} is closed, it cannot be
     * bound. Combining this equivalence with {@link ServerSocket#isBound()} is assumed to yield a
     * very good approximation to the actual state of the server socket.
     *
     * @return <code>true</code>, if this server is bound; <code>false</code> otherwise.
     * @see Server#bind(BindInformation)
     * */
    @Override
    public boolean isBound() {
        if (this.socket != null)
            return (this.socket.isBound() && !this.socket.isClosed());
        return false;
    }

    /**
     * Accepts an incoming client connection. If there is no incoming connection when calling this
     * method, it will block until there is a connection or a specified timeout is hit.
     *
     * Notice that {@link ServerSocket#accept()} returns {@link Socket}, which is fed into
     * {@link NetworkEndPoint#NetworkEndPoint(Socket)}. This will be wired up with this server's
     * protocol and configurations to construct a {@link Client}.
     *
     * This method should be called in {@link Server#acceptLoopIteration()} to ensure that client
     * connections are accepted repeatedly.
     *
     * In this implementation, each server - side client will be assigned a message - loop that
     * repeatedly calls {@link Client#receive()}. This results in a request - response model, because
     * each server - side client can only react to messages via {@link CommunicationListener#onReceive(Client, Message)}.
     *
     * @return Accepted client.
     * @throws AcceptException If accepting a client via {@link ServerSocket#accept()} fails.
     * @throws ConnectionException If, among others, creating an endpoint fails. Is also triggered
     *                             when trying to connect the client with a handshake or receiving
     *                             messages in the message - loop.
     * @throws InvalidClientException If client - side client posted invalid functionality.
     * @throws MessageParserException If parsing a message fails. Can be caused by {@link Client#connect(ConnectionInformation)}
     *                                due to {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}
     *                                or in the message - loop by {@link Client#receive()}.
     * @throws TimeoutException If accepting clients via {@link ServerSocket#accept()} times out.
     * @see ClientType
     * @see Client
     * @see CommunicationListener
     * */
    @Override
    public Client accept() throws CommunicationException {

        if (!this.isBound())
            throw new AcceptException("Server needs to be bound in order to accept clients.");

        Client client;

        // Accept incoming connection.
        Socket clientSocket;
        try {
            clientSocket = this.socket.accept();
        } catch (SocketTimeoutException e) {
            throw new TimeoutException("Server socket timed out while waiting for connection(s).", e);
        } catch (IOException e) {
            throw new AcceptException("Server socket failed accepting connection(s).", e);
        }

        // Construct endpoint
        EndPoint ep;
        try {
            ep = new NetworkEndPoint(clientSocket);
        } catch (ConnectionException e) {

            // Deadly error(s)! We need to clean up socket returned by ServerSocket#accept
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            throw e;
        }

        try {
            // Define server - side of connection
            client = new NetworkClient(ep);
            client.setProtocol(this.getProtocol());
            client.setType(ClientType.MANAGER);
            client.setConfiguration(this.getClientConfiguration());

            // Take all listeners registered with the server and assign them to the server-side
            // part of the connection with the client.
            for (CommunicationListener listener : this.getListeners())
                client.addCommunicationListener(listener);

            // Start client, i.e. perform handshake and run message loop.
            client.connect(null);

            // Run client in message loop, i.e. request - response loop
            final Thread t = new Thread(() -> {
                boolean run = true;

                while (run) {

                    try {
                        client.receive();
                    } catch (MessageParserException ignored) {

                    } catch (CommunicationException e) {
                        run = false;
                    }
                }

                client.disconnect();
                getClients().remove(client);
            });
            this.messageThreads.add(t);
            t.start();

        } catch (CommunicationException e) {

            // Deadly error(s)! We need to clean up socket returned by ServerSocket#accept and
            // also shutdown endpoint -> EndPoint#disconnect handles both!
            ep.disconnect();
            throw e;
        }

        return client;
    }

    /**
     * Closes this server by interrupting all message - loop threads, disconnecting server - side
     * clients and closing this server socket.
     *
     * @see ServerSocket#close()
     * */
    @Override
    public void close() {

        for (Thread thread : this.messageThreads)
            thread.interrupt();

        for (Client client : this.getClients()) {
            if (client != null)
                client.disconnect();
            this.getClients().remove(client);
        }

        try {
            if (this.socket != null)
                this.socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Sets the server configuration to use. This, among other things, allows tuning timeouts and
     * the number of clients. Changing the configuration after starting the server with {@link Server#start()}
     * or {@link Server#startAsync()} may cause crashes!
     *
     * @param configuration New configuration to use for this server.
     * @see ServerConfiguration
     * */
    @Override
    public void setConfiguration(Configuration configuration) {
        // Check if configuration is a subclass of EndPointConfiguration
        if (ServerConfiguration.class.isAssignableFrom(configuration.getClass())) {
            this.configuration = (ServerConfiguration) configuration;
            try {
                this.socket.setSoTimeout(this.configuration.getTimeout());
            } catch (SocketException ignored) {}
        }
    }

    /**
     * Returns this server's configuration, which contains information that is vital to handling
     * client connections.
     *
     * @return Server configuration.
     * @see ServerConfiguration
     * */
    @Override
    @NonNull
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Sets the default server - side client configuration. It will be assigned to any client that
     * is constructed from an incoming client connection.
     *
     * CAUTION: It also affects server - side clients that are currently up and running.
     *
     * @param configuration New client configuration to use for any future server - side client.
     * @see ClientConfiguration
     * */
    public void setClientConfiguration(ClientConfiguration configuration) {
        this.clientConfiguration = configuration;

        for (final Client client : this.getClients()) {
            if (client != null)
                client.setConfiguration(configuration);
        }
    }

    /**
     * Returns this server's default client configuration.
     *
     * @return Server - side client configuration.
     * @see ClientConfiguration
     * */
    public ClientConfiguration getClientConfiguration() {
        return this.clientConfiguration;
    }

    /**
     * Performs one iteration of the accept - loop by just calling {@link NetworkServer#accept()}.
     *
     * It adds the newly accepted client to a list of clients that connected successfully.
     *
     * @throws AcceptException If accepting a client via {@link ServerSocket#accept()} fails.
     * @throws ConnectionException If, among others, creating an endpoint fails. Is also triggered
     *                             when trying to connect the client with a handshake or receiving
     *                             messages in the message - loop.
     * @throws InvalidClientException If client - side client posted invalid functionality.
     * @throws MessageParserException If parsing a message fails. Can be caused by {@link Client#connect(ConnectionInformation)}
     *                                due to {@link com.damnvulnerableapp.networking.protocol.Protocol#handshake(EndPoint, ClientType)}
     *                                or in the message - loop by {@link Client#receive()}.
     * @throws TimeoutException If accepting clients via {@link ServerSocket#accept()} times out.
     * @see NetworkServer#accept()
     * */
    @Override
    protected void acceptLoopIteration() throws CommunicationException {

        // If the client is up and running, add it to the list
        final Client client = this.accept();
        if (client == null)
            throw new AcceptException("Failed to accept client (client was null).");
        this.getClients().add(client);
    }
}
