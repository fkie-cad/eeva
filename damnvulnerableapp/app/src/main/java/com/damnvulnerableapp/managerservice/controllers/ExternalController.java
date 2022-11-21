package com.damnvulnerableapp.managerservice.controllers;

import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.managerservice.controllers.states.VulnerableAppState;
import com.damnvulnerableapp.managerservice.controllers.states.VulnerableSelectState;
import com.damnvulnerableapp.common.exceptions.AppStateException;
import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;
import com.damnvulnerableapp.managerservice.views.ExternalView;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;

/**
 * Controller that manages the app's main logic. It handles communication with external clients
 * and keeps track of the app's state.
 *
 * To support communicating with a client, this controller implements {@link CommunicationListener},
 * which allows constructing a request - response model by just reacting to incoming messages and
 * treating them as requests. In any scenario, this controller will send a response.
 *
 * Tracking the app's state is done by simulating a DFA.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ExternalController implements CommunicationListener {

    /**
     * Amount of failures allowed when trying to send a response to the external client. If this
     * is exceeded, the application will terminate, because communication is assumed to be impossible
     * from there on.
     * */
    private static final int AMOUNT_RETRIES = 2;

    /**
     * Reference to the UI that a user is confronted with, i.e. basically a {@link com.damnvulnerableapp.networking.communication.server.NetworkServer}.
     * */
    private final ExternalView externalView;

    /**
     * Current state of app. It encapsulates behaviour that depends on the state.
     * */
    private VulnerableAppState state;

    /**
     * Process identifier of vulnerable activity.
     * */
    private int vulnerableModulePid;

    private volatile Client mainClient;

    /**
     * Constructs this controller by giving it a back - reference to the UI as well as setting
     * the initial state of the app ({@link VulnerableSelectState}).
     *
     * @param externalView Reference to UI.
     * @throws InitializationException If initializing internal client fails.
     * @see ExternalView
     * @see VulnerableAppState
     * */
    public ExternalController(ExternalView externalView) throws InitializationException {

        // Set reference to external view
        this.externalView = externalView;

        // Initialize state
        this.state = new VulnerableSelectState(this);
    }

    /**
     * Closes resources that this controller is using. Among other things, it will shutdown the view.
     * To avoid confusion, {@link ExternalView#close()} is called first. This ensures that no
     * further messages are received, which could potentially cause confusion by triggering state
     * changes.
     *
     * @see ExternalView#close()
     * */
    public final void close() {

        this.externalView.close();

        // Perform other cleanup specific to this class...
    }

    /**
     * Changes the state of the app. Every state implements allowed transitions and actions bound
     * to those transitions.
     *
     * @param state New state of the app.
     * @see VulnerableAppState
     * */
    public final void changeState(VulnerableAppState state) {
        this.state = state;
    }

    /**
     * Sets the process identifier of currently selected vulnerable module. This is used to force-
     * shutdown the module, if requested by the user.
     *
     * @param pid Process identifier of vulnerable module.
     * */
    public final void setVulnerableModulePid(int pid) {
        this.vulnerableModulePid = pid;
    }

    /**
     * Gets the process identifier of currently selected vulnerable module. This is used to force-
     * shutdown the module, if requested by the user.
     *
     * @return Process identifier of vulnerable module.
     * */
    public final int getVulnerableModulePid() {
        return this.vulnerableModulePid;
    }

    /**
     * Will be called, if a user connects to {@link ExternalView}. It is used to ensure that there
     * is only one client at once by storing the {@link Client} object of the first client that
     * connected. Any additional client will be immediately disconnected. Only after this first
     * client disconnected there may be a new client.
     *
     * @param client External client that tries to connect.
     * @see CommunicationListener
     * */
    @Override
    public final void onConnect(Client client) {

        synchronized (this) {

            if (this.mainClient != null) {

                // Allow timeout
                client.setConfiguration(new ClientExitConfiguration());
                client.disconnect();
            } else {
                this.mainClient = client;
            }
        }
    }

    /**
     * Will be called, if a connected, external client disconnects. May be called multiple times. It
     * is used to ensure that only one client is connected at a time. To that end, the client object
     * of the external client that connected first is stored and compared with any other client.
     * Only if the first client disconnects, this app will return to the SELECT state.
     *
     * @param client External client that tries to disconnect.
     * @see CommunicationListener
     * */
    @Override
    public final void onDisconnect(Client client) {

        // If a second client is disconnected
        synchronized (this) {

            // The second condition should never be true...
            if (this.mainClient != client || this.mainClient == null)
                return;

            // Exit running module, if any
            try {
                this.state.exit(null);
            } catch (InitializationException e) {

                this.sendRetry(
                        client,
                        new PlainMessage(
                                Operation.INVALID,
                                "Failed to create internal client. Shutting down..."
                        )
                );

                this.state.shutdown();
            } catch (AppStateException ignored) {}

            this.mainClient = null;
        }
    }

    /**
     * Will be called, if the server - side client that is connected to the external client, tries
     * to send a message. Beware of recursion, i.e. calling {@link Client#send(Message)} is not a
     * good idea!
     *
     * @param client Server - side client used to communicate with external client.
     * @param message Message to be sent to external client.
     * @see CommunicationListener
     * */
    @Override
    public final void onSend(Client client, Message message) {}

    /**
     * Will be called, if the external client sends a {@link PlainMessage}. Based on the {@link Operation}
     * of this message, different control paths (w.r.t. current state) will be taken. If there is
     * an invalid {@link Operation} w.r.t. current state, then an error message will be the response.
     *
     * In general, this controller tries to always send a response. If an error occurs, then the
     * response will contain information on what went wrong. If it is an internal error, e.g. a
     * {@link com.damnvulnerableapp.networking.exceptions.ConnectionException} was thrown, then
     * it is assumed that there is no way to communicate with the external client. Therefore, the
     * app will disconnect the client. There are also errors that are non - recoverable, i.e. the
     * app cannot do anything else but to terminate.
     *
     * If the operation is {@link Operation#FETCH}, then the response will contain a byte - array
     * containing data that the internal, vulnerable module wants to output.
     *
     * @param client External client that sent a message.
     * @param message Message sent by external client.
     * @see CommunicationListener
     * */
    @Override
    public final void onReceive(Client client, Message message) {

        final PlainMessage plain = (PlainMessage)message;

        try {
            switch (plain.getOperation()) {

                // Select a vulnerable module
                case SELECT:
                    this.state.select(plain);

                    this.sendRetry(
                            client,
                            new PlainMessage(
                                    Operation.SUCCESS,
                                    "Successfully selected module."
                            )
                    );
                    break;

                // Exit currently selected vulnerable module.
                case EXIT:
                    this.state.exit(plain);

                    this.sendRetry(
                            client,
                            new PlainMessage(
                                    Operation.SUCCESS,
                                    "Successfully closed module."
                            )
                    );
                    break;

                // Shut down this app.
                case SHUTDOWN:
                    this.state.shutdown();
                    break;

                // Forward data to vulnerable module.
                case FORWARD:
                    this.state.forward(plain);
                    this.sendRetry(
                            client,
                            new PlainMessage(
                                    Operation.SUCCESS,
                                    "Successfully forwarded message."
                            )
                    );
                    break;

                // Send data output by vulnerable module to client.
                case FETCH:
                        this.sendRetry(
                                client,
                                this.state.fetch(plain)
                        );
                    break;

                default:
                    this.sendRetry(
                            client,
                            new PlainMessage(
                                    Operation.INVALID,
                                    "Invalid operation: " + plain.getOperation()
                            )
                    );
            }
        } catch (InvalidTransitionException | VulnerableModuleException e) {
            // These exceptions are recoverable
            this.sendRetry(
                    client,
                    new PlainMessage(
                            Operation.INVALID,
                            e.getMessage()
                    )
            );
        } catch (AppStateException | InitializationException e) {
            // There exceptions are deadly to app
            this.sendRetry(
                    client,
                    new PlainMessage(
                            Operation.INVALID,
                            e.getMessage()
                    )
            );
            this.state.shutdown();
        }
    }

    /**
     * Will be called, if external client sends an invalid message, i.e. if the sent message is
     * declared as invalid by {@link com.damnvulnerableapp.networking.messages.PlainMessageParser}.
     *
     * The external client will be informed that the message was invalid. If informing the external
     * client fails, then the app will shut down.
     *
     * @param client External client that sent an invalid message.
     * @param message Invalid message.
     * @see CommunicationListener
     * @see com.damnvulnerableapp.networking.messages.PlainMessageParser
     * */
    @Override
    public final void onInvalidMessage(Client client, byte[] message) {
        this.sendRetry(client, new PlainMessage(Operation.INVALID, "Invalid message: " + new String(message)));
    }

    /**
     * Tries to send a message via a client. If sending the message fails, it will be retried as
     * often as possible (i.e. until a threshold is hit). If the threshold is hit, then the app will
     * disconnect the client, because it is assumed for communication to be impossible from there on.
     *
     * @param client Client to send message to.
     * @param message Message to send.
     * */
    private void sendRetry(Client client, PlainMessage message) {

        // Try to send message AMOUNT_RETRIES times
        int amountRetries = 0;
        while (amountRetries < ExternalController.AMOUNT_RETRIES) {
            try {
                client.send(message);
                return;
            } catch (CommunicationException e) {
                amountRetries++;
            }
        }

        // If sending the message fails: client will be disconnected
        client.disconnect();
    }
}
