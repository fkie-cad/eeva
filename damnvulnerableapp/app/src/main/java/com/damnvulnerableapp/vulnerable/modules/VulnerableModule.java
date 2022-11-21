package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;
import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;
import com.damnvulnerableapp.vulnerable.VulnerableActivity;
import com.damnvulnerableapp.vulnerable.controller.InternalController;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Superclass of vulnerable modules that load JNI code, which is susceptible to e.g. buffer overflows.
 * This class handles I/O between {@link com.damnvulnerableapp.vulnerable.controller.InternalController}
 * and any subclass of this class.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public abstract class VulnerableModule {

    /**
     * Input from the perspective of the vulnerable module. This is where every user - input lands.
     * */
    private final BlockingQueue<byte[]> inputQueue;

    /**
     * Output from the perspective of the vulnerable module. This is where everything goes that
     * the module wants to tell the user.
     * */
    private final BlockingQueue<byte[]> outputQueue;

    /**
     * Configuration that determines e.g. timeouts. This is mandatory when implementing a new module.
     * */
    private final VulnerableConfiguration configuration;

    /**
     * Construct module by setting up I/O queues and assigning a configuration to this module
     *
     * @param configuration Configuration to use for this module.
     * */
    public VulnerableModule(VulnerableConfiguration configuration) {
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();

        this.configuration = configuration;
    }

    /**
     * Forwards a message to the vulnerable module, i.e. append the message to the input queue of
     * the vulnerable module. It is NOT allowed to forward <code>null</code>.
     *
     * If an {@link InterruptedException} occurs, this means that {@link InternalController#close()}
     * is being called. This again implies that this app will die/shutdown soon.
     *
     * If the queue is full, then this method may time out if {@link VulnerableConfiguration#getForwardTimeout()}
     * returns a negative value, which results in a call to {@link BlockingQueue#put(Object)} instead
     * of {@link BlockingQueue#offer(Object, long, TimeUnit)}. The timeout is defined in milliseconds.
     *
     * @param message Message to forward to vulnerable module.
     * @throws VulnerableModuleOperationException If {@link BlockingQueue#offer(Object, long, TimeUnit)}
     *                                            times out.
     * */
    public final void forward(byte[] message) throws VulnerableModuleOperationException {

        try {
            if (this.configuration.getForwardTimeout() < 0) {
                this.inputQueue.put(message);
            } else {
                if (!this.inputQueue.offer(message, this.configuration.getForwardTimeout(), TimeUnit.MILLISECONDS))
                    throw new VulnerableModuleOperationException("Timed out while forwarding data.");
            }
        } catch (InterruptedException ignored) {
            // Only reason for this to happen is InternalController#close
        }
    }

    /**
     * Fetches a message from the vulnerable module, i.e. take a message from the output queue of
     * the vulnerable module. If there is no message available, this method will block until there is
     * or a timeout is hit. The timeout is in milliseconds.
     *
     * If an {@link InterruptedException} occurs, this means that {@link InternalController#close()}
     * is being called. This again implies that this app will die/shutdown soon.
     *
     * If {@link VulnerableConfiguration#getFetchTimeout()} returns a negative value, then this
     * is interpreted as an infinite timeout and will therefore trigger {@link BlockingQueue#take()}
     * instead of {@link BlockingQueue#poll(long, TimeUnit)}.
     *
     * @return Message from vulnerable module.
     * @throws VulnerableModuleOperationException If {@link BlockingQueue#poll(long, TimeUnit)} times
     *                                            out.
     * */
    public final byte[] fetch() throws VulnerableModuleOperationException {

        byte[] data = null;
        try {
            if (this.configuration.getFetchTimeout() < 0) {
                data = this.outputQueue.take();
            } else {
                data = this.outputQueue.poll(this.configuration.getFetchTimeout(), TimeUnit.MILLISECONDS);
                if (data == null)
                    throw new VulnerableModuleOperationException("Timed out while fetching data.");
            }
        } catch (InterruptedException ignored) {
            // Only reason for this to happen is InternalController#close
        }

        return data;
    }

    /**
     * Returns whether there is still data to be fetched from vulnerable module.
     *
     * @return <code>true</code>, if there is still data available; <code>false</code> otherwise.
     * */
    public final boolean isFetchable() {
        return !this.outputQueue.isEmpty();
    }

    /**
     * Entry point of all implementations of this class. Technically, this can be as simple as a
     * "Hello World" - program or as complex as a complex app (notice {@link VulnerableActivity#getInstance()}).
     *
     * @throws VulnerableModuleException If any module related error occurs. This can be ignored.
     * */
    public abstract void main() throws VulnerableModuleException;

    /**
     * Adds a message to the output queue of this vulnerable module. This is comparable to e.g.
     * printing to console.
     *
     * If an {@link InterruptedException} occurs, this means that {@link InternalController#close()}
     * is being called. This again implies that this app will die/shutdown soon.
     *
     * If {@link VulnerableConfiguration#getOutputTimeout()} returns a negative value, then
     * {@link BlockingQueue#put(Object)} will be called instead of {@link BlockingQueue#offer(Object, long, TimeUnit)}.
     *
     * @param message Message to output. Can be <code>null</code>, in which case an empty byte array
     *                is sent.
     * @throws VulnerableModuleOperationException If {@link BlockingQueue#offer(Object, long, TimeUnit)}
     *                                            timed out.
     * */
    protected final void output(byte[] message) throws VulnerableModuleOperationException {
        try {
            if (message == null)
                message = new byte[0];
            if (this.configuration.getOutputTimeout() < 0) {
                this.outputQueue.put(message);
            } else {
                if (!this.outputQueue.offer(message, this.configuration.getOutputTimeout(), TimeUnit.MILLISECONDS))
                    throw new VulnerableModuleOperationException("Timed out while outputting data.");
            }
        } catch (InterruptedException ignored) {
            // Only reason for this to happen is InternalController#close
        }
    }

    /**
     * Takes a message from the input queue of this vulnerable module. This is comparable to e.g.
     * read user input from console.
     *
     * If an {@link InterruptedException} occurs, this means that {@link InternalController#close()}
     * is being called. This again implies that this app will die/shutdown soon.
     *
     * If {@link VulnerableConfiguration#getInputTimeout()} returns a negative value, then
     * {@link BlockingQueue#take()} will be called instead of {@link BlockingQueue#poll(long, TimeUnit)}.
     *
     * @return Received message.
     * @throws VulnerableModuleOperationException If {@link BlockingQueue#poll(long, TimeUnit)} timed
     *                                            out.
     * */
    protected final byte[] input() throws VulnerableModuleOperationException {

        byte[] data = null;
        try {
            if (this.configuration.getInputTimeout() < 0) {
                data = this.inputQueue.take();
            } else {
                data = this.inputQueue.poll(this.configuration.getInputTimeout(), TimeUnit.MILLISECONDS);
                if (data == null)
                    throw new VulnerableModuleOperationException("Timed out while waiting for input.");
            }
        } catch (InterruptedException ignored) {
            // Only reason for this to happen is InternalController#close
        }

        return data;
    }
}
