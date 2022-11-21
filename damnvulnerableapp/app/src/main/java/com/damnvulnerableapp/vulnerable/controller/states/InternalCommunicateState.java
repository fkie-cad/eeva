package com.damnvulnerableapp.vulnerable.controller.states;

import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.controller.InternalController;
import com.damnvulnerableapp.vulnerable.modules.VulnerableConfiguration;
import com.damnvulnerableapp.vulnerable.modules.VulnerableModule;

/**
 * App state that implements communication between an external client (manager app) and a vulnerable
 * module. This state acts as a sink, i.e. there is no other state to which a transition is made.
 * Therefore, messages of type {@link Operation#SELECT} will be denied, as the vulnerable module is
 * assumed to be running already.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class InternalCommunicateState extends InternalState {

    /**
     * Constructs this state by assigning the context.
     *
     * @param context Context of this state.
     * @see InternalController
     * */
    public InternalCommunicateState(InternalController context) {
        super(context);
    }

    /**
     * Always triggers an {@link InvalidTransitionException}. It is not allowed to re-select a
     * module, i.e. to restart a module (maybe in the future). This is due to the fact that once
     * {@link VulnerableModule#main()} is running, everything from overwriting return addresses to
     * crashing the JVM can happen. Therefore, restarting the module in this process is not supported,
     * as this process acts as a one - way ticket.
     *
     * @param message {@link PlainMessage} that triggered the selection process. It has the type
     *                                    {@link com.damnvulnerableapp.networking.messages.Operation#SELECT}.
     * @throws InvalidTransitionException Always.
     * */
    @Override
    public void select(PlainMessage message) throws InvalidTransitionException {
        throw new InvalidTransitionException("Cannot SELECT new module, if there is already a module running.");
    }

    /**
     * Tries to forward a message to the vulnerable module. To that end, this method will first
     * check whether {@link VulnerableModule#main()} is still running and if this is the case will
     * simply forward the message via {@link VulnerableModule#forward(byte[])}.
     *
     * In case the module stopped, i.e. {@link Thread#isAlive()} returns <code>false</code>, it will
     * check whether there is still data to be fetched from the module. If so, the communication
     * partner will be informed. Otherwise, this app will terminate as it fulfilled its purpose.
     *
     * @param message Message that triggered the forwarding process. It has the type
     *                {@link com.damnvulnerableapp.networking.messages.Operation#FORWARD}
     * @throws VulnerableModuleOperationException If {@link VulnerableModule#main()} stopped and
     *                                            there is additional data to be fetched, or forwarding
     *                                            timed out.
     * */
    @Override
    public void forward(PlainMessage message) throws VulnerableModuleOperationException {

        final Thread moduleThread = this.getContext().getModuleThread();
        final VulnerableModule module = this.getContext().getModule();

        if (!moduleThread.isAlive()) {

            if (!module.isFetchable())
                this.exit();
            else
                throw new VulnerableModuleOperationException("Module terminated. Try fetching remaining data.");
        } else {

            // Content is always not null, as message successfully passed parser in external controller.
            module.forward(message.getParameters().get(Parameter.CONTENT));
        }
    }

    /**
     * Tries to fetch data from the vulnerable module. If there is nothing to fetch, this app will
     * terminate. Otherwise, if fetching does not time out, the data will be returned.
     *
     * Timeout values are to be defined in {@link VulnerableConfiguration},
     * which is mandatory to implement alongside any module. If {@link VulnerableConfiguration#getFetchTimeout()}
     * returns a negative value, the timeout will be infinite.
     *
     * @param message Message that triggered the forwarding process. It has the type
     *                {@link com.damnvulnerableapp.networking.messages.Operation#FETCH}
     * @return {@link PlainMessage} of type {@link Operation#SUCCESS} that wraps the output data.
     * @throws VulnerableModuleOperationException If fetching timed out. It is also returned after
     *                                            a call to {@link InternalCommunicateState#exit()}
     *                                            when the thread stopped and there is nothing left
     *                                            to fetch.
     * */
    @Override
    public PlainMessage fetch(PlainMessage message) throws VulnerableModuleOperationException {

        final Thread moduleThread = this.getContext().getModuleThread();
        final VulnerableModule module = this.getContext().getModule();

        // If module#main terminated and there is nothing left to fetch:
        if (!moduleThread.isAlive() && !module.isFetchable()) {
            this.exit();
            throw new VulnerableModuleOperationException("Module terminated."); // <-- will never reach user
        }

        return new PlainMessage(Operation.SUCCESS, module.fetch());
    }
}
