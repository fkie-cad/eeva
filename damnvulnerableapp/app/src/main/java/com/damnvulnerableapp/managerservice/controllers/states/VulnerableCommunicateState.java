package com.damnvulnerableapp.managerservice.controllers.states;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleExitException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;
import com.damnvulnerableapp.managerservice.ManagerService;
import com.damnvulnerableapp.managerservice.controllers.ExternalController;
import com.damnvulnerableapp.common.exceptions.InvalidTransitionException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleCommunicationException;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * COMMUNICATE - state that will be in use, if a vulnerable module is selected. This is the main
 * state. Therefore, the only way to leave this state is to either close the vulnerable module or
 * to shut down the app.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class VulnerableCommunicateState extends VulnerableAppState {

    /**
     * Interval, at which to check for existence of vulnerable module process.
     * */
    private static final int WAIT_INTERVAL = 200;

    /**
     * State of vulnerable module, i.e. either its process is up and running or not. This flag is
     * used to wait until the process of the vulnerable module dies before returning from
     * {@link VulnerableCommunicateState#exit(PlainMessage)}.
     * */
    private volatile boolean moduleAlive;

    /**
     * Constructs this state by storing the reference to the state manager. Coming into this state
     * implies that the vulnerable module is up and running.
     *
     * @param context State manager, i.e. the context.
     * @param internalClient Initialized internal client. As this state can only be reached through
     *                       {@link VulnerableSelectState}, this client will be coming from {@link VulnerableSelectState}.
     * @throws InitializationException If initializing internal client fails.
     * */
    public VulnerableCommunicateState(ExternalController context, Client internalClient) throws InitializationException {
        super(context, internalClient);

        // Module is assumed to be alive!
        this.moduleAlive = true;
    }

    /**
     * Invalid operation in this state. It must not possible to select a module, if there is already
     * a selected module. In order to select a different module/restart the current module, try to
     * EXIT first and (re - )SELECT the module of choice.
     *
     * @param ignored Request that triggered this routine. Will be ignored.
     * @throws InvalidTransitionException Always thrown, because this operation is not permitted in
     *                                    this state, i.e. in the SELECT - state.
     * */
    @Override
    public final void select(PlainMessage ignored) throws InvalidTransitionException {
        throw new InvalidTransitionException(
                "Cannot SELECT in COMMUNICATE - state. There is already a selected module."
        );
    }

    /**
     * Exits selected, vulnerable module. To that end, this method first sets the default timeout
     * to a value that allows the client to timeout quickly (i.e. e.g. no 20s waiting). Then it gives
     * the module a chance to terminate itself. Finally the internal client will be disconnected
     * and the module killed via {@link android.os.Process#killProcess(int)}. Eventually, the state
     * will be changed back to SELECT.
     *
     * After this call, the process identifier of the vulnerable module will be reset to 0.
     *
     * @param message Request that triggered this routine; or <code>null</code>.
     * @throws InitializationException If initializing internal client fails.
     * */
    @Override
    public final void exit(PlainMessage message) throws InitializationException {

        // In order for this not to hang/block, set the timeout to something small > 0.
        this.getInternalClient().setConfiguration(this.getExitConfiguration());

        try {
            // Tell module to quit. If this fails, the module most likely crashed so it does not matter
            if (message == null)
                message = new PlainMessage(Operation.EXIT, "rip");
            this.getInternalClient().send(message);

            // Give module a chance to terminate itself. This will trigger a ConnectionException
            // in almost all cases.
            this.getInternalClient().receive();
        } catch (CommunicationException ignored) {}

        // Disconnect internal client
        this.getInternalClient().disconnect();

        // Kill vulnerable process
        if (this.getContext().getVulnerableModulePid() != 0)
            Process.killProcess(this.getContext().getVulnerableModulePid());

        this.spinOnVulnerableModuleAlive();

        // Reset pid
        this.getContext().setVulnerableModulePid(0);

        // Transition back to select state
        this.getContext().changeState(new VulnerableSelectState(this.getContext()));
    }

    /**
     * Tries to forward data to the selected, vulnerable module.
     *
     * If the vulnerable module hung up or crashed, this method will run {@link VulnerableCommunicateState#exit(PlainMessage)}
     * to ensure that every resource of the module is freed and the state of the app changes back
     * to SELECT. Afterwards, an error is thrown to inform the user.
     *
     * Timeouts of 0 may introduce deadlocks, because this method waits for a response, i.e. a
     * guarantee that the data has been forwarded successfully. If the vulnerable module hangs
     * without shutting down the connection, this method might block indefinitely long.
     *
     * @param message Request that triggered this routine. It will be forwarded to the selected,
     *                vulnerable module.
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleCommunicationException If communication with the vulnerable module fails.
     *                                                This is thrown in response to any {@link CommunicationException}.
     * @throws VulnerableModuleExitException If vulnerable module terminated itself (gracefully).
     * @throws VulnerableModuleOperationException If {@link PlainMessage#getOperation()} of response
     *                                            returns something else than {@link Operation#SUCCESS}.
     * */
    @Override
    public final void forward(PlainMessage message) throws InitializationException, VulnerableModuleCommunicationException, VulnerableModuleExitException, VulnerableModuleOperationException {

        try {
            // Forward data to internal server. Operation is guaranteed to be FORWARD.
            this.getInternalClient().send(message);
            final PlainMessage response = (PlainMessage) this.getInternalClient().receive();
            if (response.getOperation() == Operation.EXIT) {

                // Handle exit from vulnerable app
                this.exit(null);
                throw new VulnerableModuleExitException("Vulnerable module terminated itself: " + response);
            } else if (response.getOperation() != Operation.SUCCESS) {
                throw new VulnerableModuleOperationException("Operation was not successful: " + response);
            }

        } catch (CommunicationException e) {

            this.exit(null);
            throw new VulnerableModuleCommunicationException("Failed to forward data.", e);
        }
    }

    /**
     * Tries to fetch data from selected, vulnerable module.
     *
     * In case the vulnerable module is not responsive, i.e. either crashed or hung up, this method
     * will run {@link VulnerableCommunicateState#exit(PlainMessage)} to ensure that every resource
     * of the module is freed and the state of the app changes back to SELECT. Afterwards, an error
     * is thrown in order to inform the user.
     *
     * Instead of forwarding the message received via the internal client, this method will
     * extract the contents. This allows for wrapping the contents in a {@link PlainMessage} with
     * {@link com.damnvulnerableapp.networking.messages.Operation#SUCCESS}.
     *
     * Timeouts of 0 may introduce deadlocks, because this method waits for data, i.e. a
     * If the vulnerable module hangs without shutting down the connection, this method might
     * block indefinitely long.
     *
     * @param message Request that triggered this routine. It will be forwarded as a request to the
     *                internal server.
     * @return Data fetched from selected, vulnerable module.
     * @throws InitializationException If initializing internal client fails.
     * @throws VulnerableModuleCommunicationException If communication with the vulnerable module fails.
     *                                                This is thrown in response to any {@link CommunicationException}.
     * @throws VulnerableModuleExitException If vulnerable module terminated itself (gracefully).
     * @throws VulnerableModuleOperationException If {@link PlainMessage#getOperation()} of response
     *                                            returns something else than {@link Operation#SUCCESS}.
     * */
    @Override
    public final PlainMessage fetch(PlainMessage message) throws InitializationException, VulnerableModuleCommunicationException, VulnerableModuleExitException, VulnerableModuleOperationException {

        try {

            // Request data from internal server. Operation is guaranteed to be FETCH
            this.getInternalClient().send(message);

            // Wait for response
            final PlainMessage response = (PlainMessage) this.getInternalClient().receive();
            if (response.getOperation() == Operation.EXIT) {

                // Handle exit from vulnerable app
                this.exit(null);
                throw new VulnerableModuleExitException("Vulnerable module terminated itself: " + response);
            } else if (response.getOperation() != Operation.SUCCESS) {
                throw new VulnerableModuleOperationException("Operation was not successful: " + response);
            }

            return response;
        } catch (CommunicationException e) {

            // Any error, under the assumption that module - specific configs are good, is deadly
            this.exit(null);
            throw new VulnerableModuleCommunicationException("Failed to fetch data.", e);
        }
    }

    /**
     * Blocks until the process identifier of the vulnerable module is no longer listed as part of
     * the list of running processes. This is somewhat a busy - wait, but a bit nicer than just
     * calling {@link Thread#sleep(long)}.
     * */
    private void spinOnVulnerableModuleAlive() {

        final Object signal = new Object();

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (!isVulnerableModuleAlive()) {
                    synchronized (signal) {
                        moduleAlive = false;
                        signal.notify();
                    }
                    this.cancel();
                }
            }
        }, 0, VulnerableCommunicateState.WAIT_INTERVAL, TimeUnit.MILLISECONDS);

        synchronized (signal) {
            while (this.moduleAlive) {
                try {
                    signal.wait();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Checks whether a process with a process identifier that matches the pid of the vulnerable
     * module exists. {@link VulnerableCommunicateState#exit(PlainMessage)} can spin on this
     * until the module is dead.
     *
     * @return <code>true</code>, if vulnerable module is alive; <code>false</code> otherwise.
     * */
    private boolean isVulnerableModuleAlive() {

        final int modulePid = this.getContext().getVulnerableModulePid();
        if (modulePid == 0)
            return false;

        final ActivityManager manager = (ActivityManager) ManagerService.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procs = manager.getRunningAppProcesses();
        if (procs != null) {
            for (final ActivityManager.RunningAppProcessInfo info : procs) {
                if (info.pid == modulePid)
                    return true;
            }
        }

        return false;
    }
}
