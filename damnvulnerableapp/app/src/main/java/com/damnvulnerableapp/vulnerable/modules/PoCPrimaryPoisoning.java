package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class PoCPrimaryPoisoning extends VulnerableModule {

    static {
        System.loadLibrary("PoCPrimaryPoisoning");
    }

    public PoCPrimaryPoisoning() {
        super(new PoCPrimaryPoisoningConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {
        byte[] message;
        byte[] result;

        this.output("free as much as you like".getBytes());
        while ((message = this.input()) != null && !new String(message).equalsIgnoreCase("EXIT")) {
            result = PoCPrimaryPoisoning.free(message);
            if (result != null)
                this.output(result);
        }
    }

    private static native byte[] free(byte[] buffer);
}
