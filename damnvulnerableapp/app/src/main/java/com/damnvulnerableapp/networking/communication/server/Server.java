package com.damnvulnerableapp.networking.communication.server;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.common.configuration.Configurable;
import com.damnvulnerableapp.common.configuration.ServerClientConfiguration;
import com.damnvulnerableapp.common.configuration.ServerConfiguration;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.communication.client.ConnectionInformation;
import com.damnvulnerableapp.networking.exceptions.AcceptException;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.InvalidClientException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.protocol.ServerProtocol;

import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A server that can handle {@link Client} connections. Internally it matches a client connection
 * to a {@link Client} constructed from an {@link com.damnvulnerableapp.networking.communication.client.EndPoint}.
 * This allows reusing {@link Client} and therefore this class only implements accepting clients
 * in an accept - loop.
 *
 * The server - side clients are given a protocol that is compatible with the client - side client
 * protocol, i.e. these protocols are "inverse", which again means that they do not result in
 * deadlocks and perfectly fit each other's messages.
 *
 * Because {@link Client} is used by this class, the implementation for communication can vary from
 * using network sockets to file - mappings etc.
 *
 * This class should NEVER be instantiated by calling the constructor. Rather use {@link CommunicationFactory#createServer()}
 * to get an instance of this class.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class Server implements Configurable {

    /**
     * Server - side protocol. This is the complement to the client - side protocol. Its another
     * layer on top of the raw {@link com.damnvulnerableapp.networking.communication.client.EndPoint} - layer.
     * */
    private ServerProtocol protocol;

    /**
     * List of clients that connected. This might not correspond to the list of clients that
     * are still connected, but is definitively a superset.
     * */
    private final List<Client> clients;

    /**
     * Scheduler used to run the accept - loop in certain time periods.
     * */
    private ScheduledExecutorService scheduler;

    /**
     * Configuration used for describing timeouts etc.
     * */
    protected ServerConfiguration configuration;

    /**
     * Configuration used for all server - side clients.
     * */
    protected ClientConfiguration clientConfiguration;

    /**
     * List of listeners that will be registered with ANY client.
     * */
    private final List<CommunicationListener> listeners;

    /**
     * Constructs server.
     * */
    public Server() {

        this.clients = new LinkedList<>();
        this.listeners = new LinkedList<>();

        this.configuration = new ServerConfiguration();
        this.clientConfiguration = new ServerClientConfiguration();
    }

    /**
     * Starts the server's accept - loop in a blocking manner. I.e. the calling thread will block
     * until this method returns. This is useful if the amount of clients that will connect is
     * known in advance and a thread needs to synchronize on the event that all clients connected
     * successfully.
     *
     * @throws AcceptException If internal {@link com.damnvulnerableapp.networking.communication.client.EndPoint}
     *                         failed to accept a client.
     * @throws ConnectionException If a connection error occurred, e.g. during handshake.
     * @throws InvalidClientException If a connected client is of an invalid type.
     * @throws MessageParserException If handshake fails after client connected.
     * @throws TimeoutException If receiving a message times out.
     * @see ServerConfiguration
     * */
    public void start() throws CommunicationException {

        int numClients = this.configuration.getNumClients();
        for (int i = 0; i < numClients || numClients <= 0; i++) {
            try {
                // Try to accept a client connection.
                acceptLoopIteration();
            } catch (CommunicationException e) {
                // Either error is deadly for the server, so just exit the loop
                // Actually, we need to close all client connections first
                this.close();

                throw e;
            }
        }
    }

    /**
     * Starts the server's accept - loop in a non - blocking manner. This is useful if there is an
     * unknown amount of clients that want to connect or if there is no need to synchronize on the
     * event that all clients connected successfully.
     *
     * In comparison to {@link Server#start()}, this method does not throw any exceptions as it
     * uses a separate thread to accept client connections. The exceptions will result in the
     * accept - loop to break.
     *
     * The delay, with which to call the accept - loop, is determined by this configuration object.
     *
     * @see ServerConfiguration
     * */
    public void startAsync() {

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleWithFixedDelay(new TimerTask() {
            @Override
            public void run() {
                if (clients.size() >= configuration.getNumClients() && configuration.getNumClients() > 0) {
                    this.cancel();
                    return;
                }
                try {
                    // Try to accept a client connection.
                    acceptLoopIteration();
                } catch (TimeoutException ignored) {
                    // When timing out, just ignore this client. Most likely client did not know handshake
                } catch (CommunicationException e) {
                    // Either error is deadly for the server, so just exit the loop
                    cancel();
                    scheduler.shutdown();

                    // Actually, we need to close all client connections
                    try {
                        close();
                    } catch (CommunicationException ignored) {}
                }
            }
        }, 0, this.configuration.getAcceptLoopDelay(), TimeUnit.MILLISECONDS);
    }

    /**
     * Adds an event listener to the list of listeners that will be added to a client on successful
     * connection.
     *
     * @param listener Event listener to add to any client.
     * @see CommunicationListener
     * */
    public void addCommunicationListener(CommunicationListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes an event listener from the list of listeners that will be added to a client on
     * successful connection.
     *
     * @param listener Event listener to remove.
     * @see CommunicationListener
     * */
    public void removeCommunicationListener(CommunicationListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Determines, which protocol to use for server - side clients. This method should NOT be
     * called by any other method than {@link CommunicationFactory#createServer()}. Changing the
     * protocol after successful client connection might result in crashes!
     *
     * @param protocol Server - side protocol to use for future server - side clients.
     * @see ServerProtocol
     * @see Client
     * @see CommunicationFactory#createServer()
     * */
    public void setProtocol(ServerProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Sets the default server - side client configuration. It will be assigned to any client that
     * is constructed from an incoming client connection.
     *
     * @param configuration New client configuration to use for any future server - side client.
     * @see ClientConfiguration
     * */
    public abstract void setClientConfiguration(ClientConfiguration configuration);

    /**
     * Returns this server's default client configuration.
     *
     * @return Server - side client configuration.
     * @see ClientConfiguration
     * */
    public abstract ClientConfiguration getClientConfiguration();

    /**
     * Binds this server according to the binding information. Binding may e.g. involve, but is not
     * limited to, binding a {@link java.net.ServerSocket}.
     *
     * @param info Information on where and how to bind this server.
     * @throws CommunicationException If an error regarding communication occurs. It has to be that
     *                                abstract, because there may be different approaches to
     *                                implementing a server, e.g. socket - based and file - mapping -
     *                                based. Therefore, {@link CommunicationException} is a wrapper
     *                                for all error that may occur with communication and not specific
     *                                to one implementation. {@link Client} does not need this, because
     *                                it implements {@link Client#connect(ConnectionInformation)}
     *                                itself.
     * @see Client#connect(ConnectionInformation)
     * @see BindInformation
     * */
    public abstract void bind(BindInformation info) throws CommunicationException;

    /**
     * Checks whether the server is bound, i.e. whether there has been a successful call to
     * {@link Server#bind(BindInformation)}.
     *
     * @return <code>true</code>, if this server is bound; <code>false</code> otherwise.
     * @see Server#bind(BindInformation)
     * */
    public abstract boolean isBound();

    /**
     * Accepts a client connection. Implementations of this method should add the client to
     * the list of clients that successfully connected. This method is assumed to be blocking.
     *
     * @return New server - side client, if connection is established;
     * @throws CommunicationException If a communication error occurs (see {@link Server#bind(BindInformation)}).
     * @see Server#bind(BindInformation) 
     * */
    public abstract Client accept() throws CommunicationException;

    /**
     * Closes this server. Implementations should call {@link Client#disconnect()} on each client
     * that is part of the list of clients that connected successfully. This will take care of
     * closing internal {@link com.damnvulnerableapp.networking.communication.client.EndPoint}s.
     *
     * @throws CommunicationException If any communication - related error occurs.
     * */
    public abstract void close() throws CommunicationException;

    /**
     * Gets this list of clients that successfully connected to this server, i.e. that were returned
     * by {@link Server#accept()} and called {@link Client#connect(ConnectionInformation)} without
     * any exceptions.
     *
     * Notice that this list is a superset of the list of CURRENTLY connected clients.
     *
     * @return List of clients that connected successfully.
     * @see Client
     * */
    protected synchronized List<Client> getClients() {
        return this.clients;
    }

    /**
     * Gets this server's protocol. This will be given to each server - side client and should match
     * the client - side protocol.
     *
     * @return Server - side protocol.
     * @see ServerProtocol
     * @see Server#setProtocol(ServerProtocol)
     * */
    protected ServerProtocol getProtocol() {
        return this.protocol;
    }

    /**
     * Gets the listeners that should be registered with each client that connected. This allows
     * for request - response communication.
     *
     * @return List of listeners to register with each server - side client.
     * */
    protected List<CommunicationListener> getListeners() {
        return this.listeners;
    }

    /**
     * Describes one iteration of the accept - loop that handles accepting clients. It will be
     * called repeatedly until the accept - loop stops.
     *
     * This method should ONLY perform accepting clients and not do any handshakes whatsoever. Most
     * of the time, this method will just call {@link Server#accept()}.
     *
     * It is the responsibility for a subclass to implement the accept - loop w.r.t. their base
     * implementation of communication (e.g. socket - based, file - mapping - based etc.). There
     * is no general form of an accept - loop that can handle all possible implementations for
     * communication. Also this allows fine - tuning the accept - loop.
     *
     * @throws CommunicationException If a communication error occurs (see {@link Server#bind(BindInformation)}).
     * @see Server#bind(BindInformation)
     * */
    protected abstract void acceptLoopIteration() throws CommunicationException;
}
