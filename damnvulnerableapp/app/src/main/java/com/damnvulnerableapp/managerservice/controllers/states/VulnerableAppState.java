package com.damnvulnerableapp.managerservice.controllers.states;

import android.os.Process;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.managerservice.ManagerService;
import com.damnvulnerableapp.managerservice.controllers.ExternalController;
import com.damnvulnerableapp.common.exceptions.AppStateException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;

/**
 * App state, which declares functionality used by {@link ExternalController}. It describes what
 * a specific app state needs to implement in order for the app to have state - dependent behaviour.
 * Also it implements functionality that is common among all states.
 *
 * Additionally, this class constructs the {@link Client} used to communicate with vulnerable
 * modules.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class VulnerableAppState {

    /**
     * Factory used to create the communication endpoints. By default, a {@link NetworkFactory} is
     * used for communicating with vulnerable modules.
     * @see CommunicationFactory
     * */
    private final CommunicationFactory factory;

    /**
     * Client used for communication with vulnerable module.
     * */
    private final Client internalClient;

    /**
     * Reference to the context, i.e. the portion of the app that manages the states. This can be
     * used to trigger transitions inside of states.
     * */
    private final ExternalController context;

    /**
     * Configuration used to EXIT a selected module or to shutdown the app. This ensures that
     * disconnecting the internal client does not result in a deadlock due to infinite timeout.
     * */
    private final ClientExitConfiguration exitConfiguration;

    /**
     * Constructs the app state by creating the client used to communicate with a vulnerable module
     * and storing the context.
     *
     * The client will be created using a {@link NetworkFactory} in conjunction with a
     * {@link PlainProtocolFactory}. Changing the {@link CommunicationFactory} can be achieved by
     * changing this constructor. Changing the message type is (almost) impossible, as it will be
     * used inside the vulnerable module and there is currently no way to generate messages using
     * {@link com.damnvulnerableapp.networking.protocol.ProtocolFactory}. If creation of the internal
     * client fails, this app will be shut down.
     *
     * @param context App context that manages the app states.
     * @param internalClient {@link Client} to be used as internal client. This is used in case of
     *                                     a transition from {@link VulnerableSelectState} to {@link VulnerableCommunicateState}.
     *                                     It may be <code>null</code>.
     * @throws InitializationException If initializing internal client fails.
     * @see NetworkFactory
     * @see PlainProtocolFactory
     * */
    public VulnerableAppState(ExternalController context, Client internalClient) throws InitializationException {

        // Replace with other factory, if necessary
        this.factory = new NetworkFactory();
        this.factory.setProtocolFactory(new PlainProtocolFactory());

        if (internalClient == null)
            this.internalClient = this.factory.createClient();
        else
            this.internalClient = internalClient;

        if (this.internalClient == null)
            throw new InitializationException("Failed to initialize internal client.");

        this.context = context;
        this.exitConfiguration = new ClientExitConfiguration();
    }

    /**
     * Shuts down the app. As this is a common transition that every state can perform, it is
     * defined in this class.
     *
     * @see Client#disconnect()
     * @see ExternalController#close()
     * */
    public final void shutdown() {

        // Set configuration of internal client s.t. there cannot be any infinite waits.
        this.getInternalClient().setConfiguration(this.exitConfiguration);

        try {
            // Tell module to quit. If this fails, the module most likely crashed so it does not matter
            this.getInternalClient().send(new PlainMessage(Operation.EXIT, "rip"));

            // Give module a chance to terminate itself
            this.getInternalClient().receive();
        } catch (CommunicationException ignored) {}

        // Disconnect internal client gracefully
        this.getInternalClient().disconnect();

        // Kill vulnerable process
        if (this.getContext().getVulnerableModulePid() != 0)
            Process.killProcess(this.getContext().getVulnerableModulePid());

        this.getContext().close();
        ManagerService.getInstance().stopSelf();
        Process.killProcess(Process.myPid());
    }

    /**
     * Selects a vulnerable module, if there is no selected module yet.
     *
     * @param message Request that triggered the selection process. It may contain additional
     *                information on how to run the vulnerable module.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link ExternalController#onReceive(Client, Message)}.
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleException If an error occurs while selecting the module. E.g. if the
     *                                   given module name does not match a single class.
     * @see PlainMessage
     * */
    public abstract void select(PlainMessage message) throws AppStateException, InitializationException, VulnerableModuleException;

    /**
     * Exits the currently selected, vulnerable module, if any. This might be called, if a module
     * crashed or hung up.
     *
     * @param message Request that triggered this routine. It may contain additional information on
     *                how to exit the current module. May be <code>null</code>.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link ExternalController#onReceive(Client, Message)}.
     * @throws InitializationException If initializing internal client fails.
     * @see PlainMessage
     * */
    public abstract void exit(PlainMessage message) throws AppStateException, InitializationException;

    /**
     * Tries to forward data to the vulnerable module, if any. This represents the input - portion
     * of the I/O of the vulnerable module.
     *
     * @param message Request that triggered this routine. It may contain additional information on
     *                how to forward the data.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link ExternalController#onReceive(Client, Message)}.
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleException If an error occurs inside the vulnerable module.
     * @see PlainMessage
     * */
    public abstract void forward(PlainMessage message) throws AppStateException, InitializationException, VulnerableModuleException;

    /**
     * Tries to fetch data from the vulnerable module, if any. This represents the output - portion
     * of the I/O of the vulnerable module.
     *
     * @param message Request that triggered this routine. It may contain additional information on
     *                how to fetch the data from current module.
     * @return Data from vulnerable module.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link ExternalController#onReceive(Client, Message)}.
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleException If an error occurs inside the vulnerable module.
     * @see PlainMessage
     * */
    public abstract PlainMessage fetch(PlainMessage message) throws AppStateException, InitializationException, VulnerableModuleException;

    /**
     * Gets the factory used to create the internal {@link Client}. This will be passed to the
     * vulnerable module, which allows it to construct a {@link com.damnvulnerableapp.networking.communication.server.Server}
     * that matches this internal client.
     *
     * @return Factory for constructing compatible {@link Client}s and {@link com.damnvulnerableapp.networking.communication.server.Server}s.
     * */
    protected final CommunicationFactory getFactory() {
        return this.factory;
    }

    /**
     * Gets this class's internal client that can communicate with the vulnerable module, if any.
     *
     * @return Internal client.
     * */
    protected final Client getInternalClient() {
        return this.internalClient;
    }

    /**
     * Gets the reference to the state manager. It is mainly used to trigger state transitions.
     *
     * @return State manager, i.e. context.
     * */
    protected final ExternalController getContext() {
        return this.context;
    }

    /**
     * Gets the exit configuration used to exit a selected module or to shut down the app. This
     * prevents {@link Client#disconnect()} from blocking indefinitely.
     *
     * @return Exit configuration.
     * */
    protected final ClientConfiguration getExitConfiguration() {
        return this.exitConfiguration;
    }
}
