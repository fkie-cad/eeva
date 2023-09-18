package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Assumptions:
 * 1. Pointer leak of memory region filled by an attacker.
 * 2. Pointer leak of libart.so
 * 3. Canary leak (only due to stack - based buffer overflow)
 * */

public class PoCMterpModule extends VulnerableModule {

    static {
        System.loadLibrary("PoCMterpModule");
    }

    public PoCMterpModule() {
        super(new PoCMterpModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        // Fetch user data
        byte[] message;
        ByteBuffer buffer;

        buffer = ByteBuffer.allocate(Long.BYTES).putLong(PoCMterpModule.leak());
        this.output(buffer.array());

        // Reading
        while (true) {
            message = this.input();
            if (message != null) {
                if (new String(message).equals("EXIT"))
                    break;

                long value = PoCMterpModule.readCondition(ByteBuffer.wrap(message).getLong());
                this.output(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
            } else {
                this.output("Failed to receive the message to log...Better safe than sorry!".getBytes());
            }
        }

        // Writing
        while (true) {
            message = this.input();
            if (message != null) {
                if (new String(message).equals("EXIT"))
                    break;

                long address = ByteBuffer.wrap(Arrays.copyOfRange(message,0, Long.BYTES)).getLong();
                long value = ByteBuffer.wrap(Arrays.copyOfRange(message, Long.BYTES, 2 * Long.BYTES)).getLong();
                PoCMterpModule.writeCondition(address, value);

                this.output("Bytes written".getBytes());
            } else {
                this.output("Failed to receive the message to log...Better safe than sorry!".getBytes());
            }
        }

        // Code execution
        this.output(PoCMterpModule.stackLeak());
        PoCMterpModule.bufferOverflow(this.input());


        this.output("Bye".getBytes());
        new String("test").getChars(0, 1, new char[1], 0);
        this.output("Bye 2".getBytes());
    }

    // Memory cartography and setting up bytecode
    private static native long leak();
    private static native void writeCondition(final long address, final long value);
    private static native long readCondition(final long address);

    // Simple code execution
    private static native byte[] stackLeak();
    private static native void bufferOverflow(final byte[] input);
}
