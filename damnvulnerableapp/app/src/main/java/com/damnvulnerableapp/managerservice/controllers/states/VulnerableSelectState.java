package com.damnvulnerableapp.managerservice.controllers.states;

import android.content.Intent;

import com.damnvulnerableapp.common.DynamicClassLoader;
import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleCreationException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;
import com.damnvulnerableapp.managerservice.ManagerGlobals;
import com.damnvulnerableapp.managerservice.ManagerService;
import com.damnvulnerableapp.managerservice.controllers.ExternalController;
import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.VulnerableActivity;
import com.damnvulnerableapp.vulnerable.VulnerableGlobals;
import com.damnvulnerableapp.vulnerable.modules.VulnerableModule;

/**
 * SELECT - state that is the initial state and is always in use, when a client closed a vulnerable
 * module.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class VulnerableSelectState extends VulnerableAppState {

    /**
     * Amount of connection retries to perform when selecting a module. If this is exceeded, then
     * the app will shut down.
     * */
    private static final int AMOUNT_RETRIES = 5;

    /**
     * Constructs this state by storing the reference to the state manager.
     *
     * @param context State manager, i.e. the context.
     * @throws InitializationException If initializing internal client fails.
     * */
    public VulnerableSelectState(ExternalController context) throws InitializationException {
        super(context, null);
    }

    /**
     * Selects a vulnerable module based on a message sent by an external client. The module name
     * has to be specified in the {@link Parameter#CONTENT} - field of a {@link PlainMessage}. It is
     * supposed to be in a one - to - one correspondence to a class that represents the vulnerable
     * module to load.
     *
     * For each module, there are configurations that specify e.g. timeouts for the internal client.
     * If those configurations cannot be loaded, or the class name is incorrect, then an error will
     * be thrown.
     *
     * After the vulnerable app has been started, this internal client will try to connect to the
     * {@link com.damnvulnerableapp.vulnerable.view.InternalView} and retry on failure until the
     * amount of attempted retries exceed a certain threshold. If the threshold is exceeded, it is
     * assumed that communication with the internal vulnerable module is impossible and therefore
     * this app is shut down. Otherwise, a high - level handshake is performed, in which the
     * vulnerable module communicates its process identifier. This is used to allow the manager to
     * kill the vulnerable module process, if the user requests it (e.g. by sending {@link Operation#EXIT}
     * or disconnecting).
     *
     * Finally, this method changes the state to communication.
     *
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleOperationException If dynamically loading module configurations fails.
     * */
    @Override
    public final void select(PlainMessage message) throws InitializationException, VulnerableModuleOperationException, VulnerableModuleCreationException {

        // Check content, which specifies what vulnerable module to load
        final String moduleName = new String(message.getParameters().get(Parameter.CONTENT));

        // Load module - specific configurations
        if (DynamicClassLoader.getInstance().checkClass(
                VulnerableModule.class.getPackage(),
                moduleName,
                VulnerableModule.class)) {

            final ClientConfiguration configuration = (ClientConfiguration) DynamicClassLoader.getInstance().loadClass(
                    VulnerableModule.class.getPackage(),
                    moduleName + "Configuration",
                    ClientConfiguration.class
            );
            if (configuration == null)
                throw new VulnerableModuleOperationException("Failed to load module configurations.");

            this.getInternalClient().setConfiguration(configuration);
        } else {
            throw new VulnerableModuleOperationException("Failed to find vulnerable module.");
        }

        // Load vulnerable activity
        final Intent intent = new Intent(ManagerService.getInstance(), VulnerableActivity.class);
        intent.putExtra(ManagerGlobals.FACTORY_INTENT_KEY, this.getFactory().getClass().getSimpleName());
        intent.putExtra(ManagerGlobals.MODULE_INTENT_KEY, moduleName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ManagerService.getInstance().startActivity(intent);

        // Connect internal client to vulnerable module -> could throw exception, if initialization failed
        // Might also throw exception if this method is faster than VulnerableActivity
        // -> try fixed amount of times with timeouts after each try
        int i = 0;
        while (i < VulnerableSelectState.AMOUNT_RETRIES) {

            try {

                this.getInternalClient().connect(
                        new NetworkConnectionInformation(
                                VulnerableGlobals.HOST,
                                VulnerableGlobals.PORT,
                                VulnerableGlobals.CONNECT_TIMEOUT
                        )
                );
                break;
            } catch (CommunicationException e) {
                i++;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }

        if (i >= VulnerableSelectState.AMOUNT_RETRIES) {
            //this.shutdown();
            throw new VulnerableModuleCreationException("Failed to connect to vulnerable module.");
        }

        // Send select request -> high - level handshake
        try {

            this.getInternalClient().send(
                    new PlainMessage(
                            Operation.SELECT,
                            "gogogo"
                    )
            );
        } catch (CommunicationException e) {
            //this.shutdown();
            throw new VulnerableModuleCreationException("Failed to send message.", e);
        }

        // Await response
        try {

            final PlainMessage response = (PlainMessage) this.getInternalClient().receive();
            if (response.getOperation() != Operation.SUCCESS) {
                //this.shutdown();
                throw new VulnerableModuleCreationException("Failed selecting module.");
            }

            // Content field contains pid of process
            final byte[] content = response.getParameters().get(Parameter.CONTENT);
            if (content == null) {
                //this.shutdown();
                throw new VulnerableModuleCreationException("Failed retrieving pid of vulnerable module.");
            }
            this.getContext().setVulnerableModulePid(Integer.parseInt(new String(content)));

        } catch (CommunicationException e) {
            //this.shutdown();
            throw new VulnerableModuleCreationException("Failed to receive response.", e);
        }

        // Transition to VulnerableCommunicateState, if previous steps are successful
        this.getContext().changeState(new VulnerableCommunicateState(this.getContext(), this.getInternalClient()));
    }

    /**
     * Invalid operation in this state. It must not possible to exit a module, if there is no module
     * selected.
     *
     * @param ignored Request that triggered this routine. Will be ignored.
     * @throws InvalidTransitionException Always thrown, because this operation is not permitted in
     *                                    this state, i.e. in the SELECT - state.
     * */
    @Override
    public final void exit(PlainMessage ignored) throws InvalidTransitionException {
        throw new InvalidTransitionException(
                "Cannot EXIT from SELECT - state. There is not a selected module."
        );
    }

    /**
     * Invalid operation in this state. It must not be possible to send messages to a module, if
     * there is no module selected.
     *
     * @param ignored Request that triggered this routine. Will be ignored.
     * @throws InvalidTransitionException Always thrown, because this operation is not permitted in
     *                                    this state, i.e. in the SELECT - state.
     * */
    @Override
    public final void forward(PlainMessage ignored) throws InvalidTransitionException {
        throw new InvalidTransitionException(
                "Cannot FORWARD message in SELECT - state. There is not a selected module."
        );
    }

    /**
     * Invalid operation in this state. It must not be possible to receive messages from a module, if
     * there is no module selected.
     *
     * @param ignored Request that triggered this routine. Will be ignored.
     * @return Nothing. Will always trigger {@link InvalidTransitionException}.
     * @throws InvalidTransitionException Always thrown, because this operation is not permitted in
     *                                    this state, i.e. in the SELECT - state.
     * */
    @Override
    public final PlainMessage fetch(PlainMessage ignored) throws InvalidTransitionException{
        throw new InvalidTransitionException(
                "Cannot FETCH message in SELECT - state. There is not a selected module."
        );
    }
}
