package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class PoCMemoryProbing extends VulnerableModule {

    static {
        System.loadLibrary("PoCMemoryProbing");
    }

    public PoCMemoryProbing() {
        super(new PoCMemoryProbingConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        byte[] message;
        this.output(leakHeader());
        while ((message = input()) != null && !new String(message).equals("EXIT"))
            this.output(storeInChunk(message));
    }

    private static native byte[] leakHeader();

    private static native byte[] storeInChunk(final byte[] data);
}
