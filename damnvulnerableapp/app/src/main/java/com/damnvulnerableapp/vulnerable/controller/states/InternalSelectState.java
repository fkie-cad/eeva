package com.damnvulnerableapp.vulnerable.controller.states;

import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.controller.InternalController;
import com.damnvulnerableapp.vulnerable.modules.VulnerableModule;

/**
 * Initial app state that enforces an initial {@link com.damnvulnerableapp.networking.messages.Operation#SELECT}
 * message. It does not allow to perform any other action except for exiting this app.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class InternalSelectState extends InternalState {

    /**
     * Constructs this state by assigning the context.
     *
     * @param context Context of this state.
     * @see InternalController
     * */
    public InternalSelectState(InternalController context) {
        super(context);
    }

    /**
     * Starts the vulnerable module of this app. The {@link com.damnvulnerableapp.vulnerable.modules.VulnerableModule}
     * linked to this app is given as an initial {@link android.content.Intent} at app startup. This
     * method will outsource and start {@link VulnerableModule#main()} in a separate thread.
     *
     * After the vulnerable module is up and running, this method will change the app state to
     * {@link InternalCommunicateState}.
     *
     * @param message {@link PlainMessage} that triggered the selection process. It has the type
     *                                    {@link com.damnvulnerableapp.networking.messages.Operation#SELECT}.
     * @see InternalCommunicateState
     * */
    @Override
    public void select(PlainMessage message) {

        // Start module
        final Thread moduleThread = this.getContext().getModuleThread();
        if (!moduleThread.isAlive())
            moduleThread.start();

        // Change to interactive state
        this.getContext().changeState(new InternalCommunicateState(this.getContext()));
    }

    /**
     * Always triggers an {@link InvalidTransitionException}. This transition is NOT allowed in this
     * state, as the vulnerable module has not been started yet. Therefore, trying to communicate
     * with something that is not responsive yet is denied.
     *
     * @param message Message that triggered the forwarding process. It has the type
     *                {@link com.damnvulnerableapp.networking.messages.Operation#FORWARD}
     * @throws InvalidTransitionException Always.
     * */
    @Override
    public void forward(PlainMessage message) throws InvalidTransitionException {
        throw new InvalidTransitionException("Cannot FORWARD message, if module is not running.");
    }

    /**
     * Always triggers an {@link InvalidTransitionException}. This transition is NOT allowed in this
     * state, as the vulnerable module has not been started yet. Therefore, trying to communicate
     * with something that is not responsive yet is denied.
     *
     * @param message Message that triggered the forwarding process. It has the type
     *                {@link com.damnvulnerableapp.networking.messages.Operation#FETCH}
     * @throws InvalidTransitionException Always.
     * */
    @Override
    public PlainMessage fetch(PlainMessage message) throws InvalidTransitionException {
        throw new InvalidTransitionException("Cannot FETCH message, if module is not running.");
    }
}
