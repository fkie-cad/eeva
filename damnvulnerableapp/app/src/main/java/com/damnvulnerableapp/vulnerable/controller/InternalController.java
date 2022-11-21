package com.damnvulnerableapp.vulnerable.controller;

import android.os.Process;

import com.damnvulnerableapp.common.DynamicClassLoader;
import com.damnvulnerableapp.common.exceptions.AppStateException;
import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.CommunicationListener;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.controller.states.InternalSelectState;
import com.damnvulnerableapp.vulnerable.controller.states.InternalState;
import com.damnvulnerableapp.vulnerable.modules.VulnerableModule;
import com.damnvulnerableapp.vulnerable.view.InternalView;

/**
 * Manager of this vulnerable app. It handles communication with {@link com.damnvulnerableapp.managerservice.controllers.ExternalController}
 * and translates messages into tasks to be performed on the vulnerable module. To that end, it
 * manages states of the vulnerable app in form of a DFA.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class InternalController implements CommunicationListener {

    /**
     * Amount of failures allowed when trying to send a response to the external client. If this
     * is exceeded, the application will terminate, because communication is assumed to be impossible
     * from there on.
     * */
    private static final int AMOUNT_RETRIES = 2;

    /**
     * Reference to internal view that is used to indirectly communicate with the external client.
     * */
    private final InternalView internalView;

    /**
     * Vulnerable module requested by external client.
     * */
    private VulnerableModule module;

    /**
     * Thread that runs {@link VulnerableModule#main()}. It is stored in order to allow for
     * graceful shut down of this app via {@link Thread#interrupt()}.
     * */
    private Thread moduleThread;

    /**
     * Current vulnerable app state. It determines what functionality to provide in different stages
     * of the app.
     * */
    private InternalState state;

    /**
     * Construct internal controller by assigning internal view and initializing the app state to
     * {@link InternalSelectState}.
     *
     * @param internalView Refernce to internal view.
     * @see InternalSelectState
     * */
    public InternalController(InternalView internalView) {
        this.internalView = internalView;
        this.state = new InternalSelectState(this);
    }

    /**
     * Initializes this controller by attempting to load the requested vulnerable module by name. If
     * this fails, then an error is thrown. Otherwise, a new thread is created that will run
     * {@link VulnerableModule#main()} upon receiving the first {@link Operation#SELECT} message.
     *
     * @param moduleName Name of vulnerable module to load.
     * @throws InitializationException If loading vulnerable module fails.
     * */
    public final void init(String moduleName) throws InitializationException {

        // Load module by name
        this.module = (VulnerableModule) DynamicClassLoader.getInstance().loadClass(
                VulnerableModule.class.getPackage(),
                moduleName,
                VulnerableModule.class
        );
        if (this.module == null)
            throw new InitializationException("Failed to instantiate " + moduleName);

        this.moduleThread = new Thread(() -> {
            try {
                this.module.main();
            } catch (VulnerableModuleException ignored) {}
        });
    }

    /**
     * Closes vulnerable module by shutting down {@link VulnerableModule#main()}, if still running,
     * and killing this process. Killing this process will trigger a {@link com.damnvulnerableapp.networking.exceptions.ConnectionException}
     * on the other side. Also the {@link InternalView} will be closed.
     * */
    public final void close() {

        // Stop module
        if (this.moduleThread.isAlive())
            this.moduleThread.interrupt();

        // Shut down server
        this.internalView.close();

        // Exit activity
        Process.killProcess(Process.myPid());
    }

    /**
     * Changes the current state to a given state. This e.g. allows to transition from a {@link InternalSelectState}
     * to a {@link com.damnvulnerableapp.vulnerable.controller.states.InternalCommunicateState}.
     *
     * @param state State to transition to.
     * @see InternalSelectState
     * @see com.damnvulnerableapp.vulnerable.controller.states.InternalCommunicateState
     * */
    public final void changeState(InternalState state) {
        this.state = state;
    }

    /**
     * Gets the thread that runs or will run {@link VulnerableModule#main()}. This can be used to
     * determine whether the vulnerable module is still running or whether this activity may be closed.
     *
     * @return Thread of {@link VulnerableModule#main()}
     * */
    public final Thread getModuleThread() {
        return this.moduleThread;
    }

    /**
     * Gets {@link VulnerableModule} of this vulnerable activity. Notice that the {@link VulnerableModule}
     * is fixed from the start and MUST NOT be changed. This module can be used to query information
     * on pending I/O.
     *
     * @return Selected vulnerable module.
     * */
    public final VulnerableModule getModule() {
        return this.module;
    }

    @Override
    public final void onConnect(Client client) {

    }

    /**
     * Will be called, if a client disconnects. To that end, if there is no connected client left,
     * just shut down this app.
     *
     * @param client Server - side client that wants to disconnect.
     * */
    @Override
    public final void onDisconnect(Client client) {
        // If only client disconnects, just exit
        this.state.shutdown();
    }

    @Override
    public final void onSend(Client client, Message message) {

    }

    /**
     * Will be called, if a message is received. It is assumed that there is only one client
     * connected at a time (otherwise the state can be confused). This client MUST be from the
     * manager app.
     *
     * Upon a received {@link PlainMessage}, depending on its {@link PlainMessage#getOperation()}
     * value, a state - specific operation will be performed. Observe that the first message MUST
     * have a value of {@link Operation#SELECT}. First of all, this will trigger execution of
     * {@link VulnerableModule#main()}. Secondly, the response message will contain the process
     * identifier of this app, which allows the manager app to shut down this app via
     * {@link Process#killProcess(int)}. Sending any other {@link Operation} - value as a first
     * message will just result in error responses.
     *
     * Once the module is running, any other {@link Operation} - value is allowed except for
     * {@link Operation#SELECT} (as the module is already running). Therefore, I/O with the module
     * is possible after {@link Operation#SELECT} via {@link Operation#FORWARD} and {@link Operation#FETCH}.
     * For the latter, notice that fetching a message from the output queue of the module can result
     * in a deadlock, if e.g. the manager AND the module wait for a message (mutual wait).
     *
     * Finally, exiting is the same as shutting down, i.e. {@link Operation#EXIT} and {@link Operation#SHUTDOWN}
     * have the same effect of closing this app.
     *
     * @param client Server - side client, which received the message.
     * @param message Message received.
     * */
    @Override
    public final void onReceive(Client client, Message message) {

        final PlainMessage plain = (PlainMessage) message;

        try {
            switch (plain.getOperation()) {

                case SELECT:
                    this.state.select(plain);
                    this.sendRetry(
                            client,
                            new PlainMessage(
                                    Operation.SUCCESS,
                                    Integer.toString(Process.myPid())
                            )
                    );
                    break;

                case FORWARD:
                    try {
                        this.state.forward(plain);
                        this.sendRetry(
                                client,
                                new PlainMessage(
                                        Operation.SUCCESS,
                                        "Successfully forwarded data."
                                )
                        );
                    } catch (VulnerableModuleOperationException e) {

                        this.sendRetry(
                                client,
                                new PlainMessage(
                                        Operation.SUCCESS,
                                        e.getMessage()
                                )
                        );
                    }
                    break;

                case FETCH:
                    try {
                        this.sendRetry(
                                client,
                                this.state.fetch(plain)
                        );
                    } catch (VulnerableModuleOperationException e) {
                        // e.g. fetching timed out -> not too bad
                        this.sendRetry(
                                client,
                                new PlainMessage(
                                        Operation.INVALID,
                                        e.getMessage()
                                )
                        );
                    }
                    break;

                case EXIT:
                    this.state.exit();
                    break;

                case SHUTDOWN:
                    this.state.shutdown();
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
        } catch (InvalidTransitionException e) {

            this.sendRetry(
                    client,
                    new PlainMessage(
                            Operation.INVALID,
                            e.getMessage()
                    )
            );
        } catch (AppStateException | VulnerableModuleException e) {

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
     * Will be called, if an invalid message occurs.
     *
     * If this happens (which is very unlikely), then it will inform the manager app and otherwise
     * do nothing.
     *
     * @param client Server - side client that received the invalid message.
     * @param message Invalid message in terms of {@link com.damnvulnerableapp.networking.messages.PlainMessageParser}.
     * @see com.damnvulnerableapp.networking.messages.PlainMessageParser
     * */
    @Override
    public final void onInvalidMessage(Client client, byte[] message) {
        this.sendRetry(client, new PlainMessage(Operation.INVALID, "Invalid message: " + new String(message)));
    }

    /**
     * Tries to send a message via a client. If sending the message fails, it will be retried as
     * often as possible (i.e. until a threshold is hit). If the threshold is hit, then the app will
     * terminate, because it is assumed for communication to be impossible from there on.
     *
     * @param client Client to send message to.
     * @param message Message to send.
     * */
    private void sendRetry(Client client, PlainMessage message) {

        // Try to send message AMOUNT_RETRIES times
        int amountRetries = 0;
        while (amountRetries < InternalController.AMOUNT_RETRIES) {
            try {
                client.send(message);
                return;
            } catch (CommunicationException e) {
                amountRetries++;
            }
        }

        this.state.shutdown();
    }
}
