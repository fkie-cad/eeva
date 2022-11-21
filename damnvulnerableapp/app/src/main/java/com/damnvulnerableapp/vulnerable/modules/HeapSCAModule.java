package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class HeapSCAModule extends VulnerableModule {

    static {
        System.loadLibrary("HeapSCAModule");
    }

    public HeapSCAModule() {
        super(new HeapSCAModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        byte[] message;
        byte[] result;

        this.output("Try to get the secrets >:D".getBytes());
        while ((message = this.input()) != null && !new String(message).equals("EXIT")) {
            result = HeapSCAModule.handleMessage(message);
            if (result != null)
                this.output(result);
            else
                this.output("error".getBytes());
        }
    }

    private static native byte[] handleMessage(byte[] message);
}
