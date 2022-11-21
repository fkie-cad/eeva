package com.damnvulnerableapp.managerservice.views;

import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.managerservice.controllers.ExternalController;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.server.NetworkBindInformation;
import com.damnvulnerableapp.networking.communication.server.NetworkServer;
import com.damnvulnerableapp.networking.exceptions.BindException;
import com.damnvulnerableapp.networking.exceptions.CreationException;
import com.damnvulnerableapp.networking.exceptions.InternalSocketException;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;

/**
 * User - interface in form of a {@link NetworkServer}(TCP). This is what an external user will interact
 * with when trying to use this application.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ExternalView {

    /**
     * Hostname and port, on which to bind the {@link NetworkServer}.
     * */
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    /**
     * Server that handles client connections and represents the "UI".
     * @see NetworkServer
     * */
    private final NetworkServer server;

    /**
     * Constructs the {@link NetworkServer} w.r.t. a {@link PlainProtocolFactory}. This ensures
     * that all messages are of type {@link com.damnvulnerableapp.networking.messages.PlainMessage},
     * which is a message format that supports sending binary data.
     *
     * @see NetworkFactory
     * @see NetworkServer
     * @see com.damnvulnerableapp.networking.messages.PlainMessage
     * @see PlainProtocolFactory
     * */
    public ExternalView() {

        final NetworkFactory factory = new NetworkFactory();
        factory.setProtocolFactory(new PlainProtocolFactory()); // -> always PlainMessage's
        this.server = (NetworkServer) factory.createServer();
    }

    /**
     * Initializes this view by assigning an instance of {@link ExternalController} to the server.
     * This is done by registering {@link ExternalController} as a {@link com.damnvulnerableapp.networking.communication.client.CommunicationListener}.
     *
     * Also, the {@link NetworkServer} is bound and started.
     *
     * @throws BindException If binding the {@link NetworkServer} fails.
     * @throws CreationException If creating a network resource fails.
     * @throws InitializationException If initializing internal client fails.
     * @throws InternalSocketException If an internal socket error occurs. E.g. {@link java.net.Socket#setSoTimeout(int)}
     *                                 could fail due to an internal protocol(TCP) error.
     * @see com.damnvulnerableapp.networking.communication.client.CommunicationListener
     * @see ExternalController
     * */
    public final void init() throws BindException, CreationException, InitializationException, InternalSocketException {

        // Create controller and initialize it
        final ExternalController controller = new ExternalController(this);

        // Add controller as listener to server
        this.server.addCommunicationListener(controller);

        // Bind server
        this.server.bind(new NetworkBindInformation(ExternalView.HOST, ExternalView.PORT));

        // Start server asynchronously
        this.server.startAsync();
    }

    /**
     * Closes all resources that this view manages. Primarily, the {@link NetworkServer} will be
     * closed.
     * */
    public final void close() {

        // Shutdown server
        this.server.setClientConfiguration(new ClientExitConfiguration());
        this.server.close();
    }
}
