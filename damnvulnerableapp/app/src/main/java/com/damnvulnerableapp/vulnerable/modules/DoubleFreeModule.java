package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

import java.nio.ByteBuffer;
import java.util.Locale;

public class DoubleFreeModule extends VulnerableModule {

    static {
        System.loadLibrary("DoubleFreeModule");
    }

    public DoubleFreeModule() {
        super(new DoubleFreeModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        this.output("Key - Value Storage! Most secure in this field!".getBytes());

        int index;
        while (true) {

            this.output(
                    "Send a number between 1 and 4 (0 to continue) to see one of four data templates:".getBytes()
            );

            index = ByteBuffer.wrap(this.input()).getInt();
            if (index == 0)
                break;

            this.output(DoubleFreeModule.leak(index - 1));
        }

        while (true) {
            // 1. expect name
            this.output("Please provide data to store (EXIT to end app): ".getBytes());
            byte[] input = this.input();
            if (new String(input).toUpperCase(Locale.ROOT).equals("EXIT"))
                break;

            // Call function
            DoubleFreeModule.vulnerable(input);
        }

        this.output("Terminating...".getBytes());
    }


    private native static byte[] leak(int index);
    private native static void vulnerable(byte[] input);
}
