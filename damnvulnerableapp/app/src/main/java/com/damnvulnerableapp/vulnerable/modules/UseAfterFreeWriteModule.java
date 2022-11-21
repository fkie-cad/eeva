package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

import java.nio.ByteBuffer;
import java.util.Locale;

public class UseAfterFreeWriteModule extends VulnerableModule {

    static {
        System.loadLibrary("UseAfterFreeWriteModule");
    }

    /**
     * Constructs UAF module by loading configurations.
     */
    public UseAfterFreeWriteModule() {
        super(new UseAfterFreeWriteModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        this.output("Key - Value Storage! Most secure in this field!".getBytes());

        // Prompt with default names
        int index;
        while (true) {

            this.output(
                    "Send a number between 1 and 4 (0 to continue) to see one of four key name templates:".getBytes()
            );

            index = ByteBuffer.wrap(this.input()).getInt();
            if (index == 0)
                break;

            this.output(this.lookupExamples(index - 1));
        }

        while (true) {
            // 1. expect name
            this.output("Please provide the key name (EXIT to end app): ".getBytes());
            byte[] name = this.input();
            if (new String(name).toUpperCase(Locale.ROOT).equals("EXIT"))
                break;

            // 2. expect value
            this.output("Please provide the key value: ".getBytes());
            long value = ByteBuffer.wrap(this.input()).getLong();

            // Call function
            this.storePair(name, value);
            this.output(("Successfully stored (" + new String(name) + ":" + value + ")!").getBytes());
        }

        this.output("Terminating...".getBytes());
    }

    private native byte[] lookupExamples(int index);
    private native void storePair(byte[] name, long value);
}
