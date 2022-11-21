package com.damnvulnerableapp.vulnerable.view;

import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.common.exceptions.MVCException;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.communication.server.NetworkBindInformation;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.vulnerable.VulnerableGlobals;
import com.damnvulnerableapp.vulnerable.controller.InternalController;

/**
 * Communication interface for manager. It will provide means for communication with {@link com.damnvulnerableapp.managerservice.controllers.ExternalController}
 * via a request - response model.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class InternalView {

    /**
     * Internal server, which matches the client "on the other side".
     * */
    private final Server server;

    /**
     * Construct view by assigning the factory.
     *
     * @param factory Factory that creates matching clients and servers.
     * */
    public InternalView(CommunicationFactory factory) {
        this.server = factory.createServer();
    }

    /**
     * Initializes this internal view by setting up the internal server and initializing the
     * {@link com.damnvulnerableapp.vulnerable.controller.InternalController}.
     *
     * @param moduleName Name of the vulnerable module to load.
     * @throws CommunicationException If an error occurs while trying to bind the server.
     * @throws MVCException If initializing the controller fails.
     * */
    public final void init(String moduleName) throws CommunicationException, MVCException {

        // Initialize controller
        final InternalController controller = new InternalController(this);
        controller.init(moduleName);

        // Start server
        this.server.addCommunicationListener(controller);
        this.server.bind(new NetworkBindInformation(VulnerableGlobals.HOST, VulnerableGlobals.PORT));
        this.server.startAsync();
    }

    /**
     * Closes resources used by this view.
     * */
    public final void close() {
        try {
            this.server.setClientConfiguration(new ClientExitConfiguration());
            this.server.close();
        } catch (CommunicationException ignored) {}
    }
}
