package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleOperationException;

import java.nio.ByteBuffer;

/**
 * Module that uses a JNI library that is susceptible to a stack - based buffer overflow
 * vulnerability. It behaves like an echo service, i.e. it expects input, applies a vulnerable
 * JNI function to that input and echoes back the modified input.
 *
 * As this is the easy version of stack - based buffer overflows, it will expect an additional
 * parameter that makes it easier to leak data.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class EasyStackBufferOverflowModule extends VulnerableModule {

    static {
        // Load vulnerable jni code
        System.loadLibrary("EasyStackBufferOverflowModule");
    }

    /**
     * Construct module. This also links {@link StackBufferOverflowModuleConfiguration} to this
     * module.
     */
    public EasyStackBufferOverflowModule() {
        super(new StackBufferOverflowModuleConfiguration());
    }

    /**
     * Performs the echoing of arbitrary messages after applying a vulnerable function.
     *
     * @throws VulnerableModuleOperationException If {@link VulnerableModule#input()} or
     *                                            {@link VulnerableModule#output(byte[])} times out.
     * */
    @Override
    public final void main() throws VulnerableModuleOperationException {

        this.output("Welcome to the latest version of the echo service >:)".getBytes());

        byte[] message;
        int unknown;
        do {
            message = this.input();
            unknown = ByteBuffer.wrap(this.input()).getInt();
            byte[] upper = this.vulnerableToUpper(message, unknown);

            this.output(upper);
        } while (!new String(message).equals("EXIT"));

        this.output("Exiting...".getBytes());
    }

    /**
     * Vulnerable JNI function that converts any string to uppercase. This looks pretty innocent,
     * right?
     *
     * @param string String, whose characters to convert to uppercase.
     * @param unknown Some unknown parameter, whose impact one should try to figure out. Might be
     *                related to leaking data.
     * @return String in uppercase.
     * */
    private native byte[] vulnerableToUpper(byte[] string, int unknown);
}
