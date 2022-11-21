package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class SecondaryFakeModule extends VulnerableModule {

    static {
        System.loadLibrary("SecondaryFakeModule");
    }

    public SecondaryFakeModule() {
        super(new SecondaryFakeModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        byte[] message;
        byte[] result;

        this.output("free as much as you like".getBytes());
        while ((message = this.input()) != null && !new String(message).toUpperCase().equals("EXIT")) {
            result = SecondaryFakeModule.free(message);
            if (result != null)
                this.output(result);
        }
    }

    private static native byte[] free(byte[] chunk);
}