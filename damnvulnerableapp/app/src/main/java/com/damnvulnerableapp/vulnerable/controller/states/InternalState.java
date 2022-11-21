package com.damnvulnerableapp.vulnerable.controller.states;

import com.damnvulnerableapp.common.exceptions.AppStateException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.controller.InternalController;

/**
 * Abstract description of a state in the vulnerable app. It provides the interface used by
 * {@link InternalController#onReceive(Client, Message)} and others to perform actions based on
 * events.
 *
 * Notice that this is part of a DFA, i.e. this describes exactly one state of a DFA. Depending on
 * the state, performing certain actions is allowed or not or may result in different responses.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class InternalState {

    /**
     * Reference to the controller. This allows for state changes to happen in any of the transition
     * methods. This context may provide methods to access data structures that are useful for
     * implementing any transition methods.
     * */
    private final InternalController context;

    /**
     * Constructs this state by assigning the context.
     *
     * @param context Context to use for this state.
     * @see InternalController
     * */
    public InternalState(InternalController context) {
        this.context = context;
    }

    /**
     * Exits this app. This is the same as shutting this app down.
     * */
    public final void exit() {
        this.shutdown();
    }

    /**
     * Shuts this app down by calling {@link InternalController#close()}. Mainly this will kill this
     * app's process via {@link android.os.Process#killProcess(int)}.
     * */
    public final void shutdown() {
        this.context.close();
    }

    /**
     * Selects a vulnerable module, i.e. starts the module thread.
     *
     * @param message Request that triggered the selection process. It may contain additional
     *                information on how to run the vulnerable module.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link InternalController#onReceive(Client, Message)}.
     * @see PlainMessage
     * */
    public abstract void select(PlainMessage message) throws AppStateException;

    /**
     * Tries to forward data to the vulnerable module, if any. This represents the input - portion
     * of the I/O of the vulnerable module.
     *
     * @param message Request that triggered this routine. It may contain additional information on
     *                how to forward the data.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link InternalController#onReceive(Client, Message)}.
     * @throws VulnerableModuleException If an error occurs inside the vulnerable module.
     * @see PlainMessage
     * */
    public abstract void forward(PlainMessage message) throws AppStateException, VulnerableModuleException;

    /**
     * Tries to fetch data from the vulnerable module, if any. This represents the output - portion
     * of the I/O of the vulnerable module.
     *
     * @param message Request that triggered this routine. It may contain additional information on
     *                how to fetch the data from current module.
     * @return Data from vulnerable module.
     * @throws AppStateException If a DFA - related error occurs. E.g. if trying to make a transition
     *                           which is not allowed in a certain state. This will be used to manage
     *                           messaging in {@link InternalController#onReceive(Client, Message)}.
     * @throws VulnerableModuleException If an error occurs inside the vulnerable module.
     * @see PlainMessage
     * */
    public abstract PlainMessage fetch(PlainMessage message) throws AppStateException, VulnerableModuleException;

    /**
     * Gets the context assigned to this state at construction time. This allows to perform state
     * changes via {@link InternalController#changeState(InternalState)} inside of one of the state
     * methods like {@link InternalState#select(PlainMessage)}.
     *
     * @return Context of this state.
     * */
    protected final InternalController getContext() {
        return this.context;
    }
}
