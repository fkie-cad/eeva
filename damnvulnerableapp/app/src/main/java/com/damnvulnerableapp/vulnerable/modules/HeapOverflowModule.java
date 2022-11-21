package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class HeapOverflowModule extends VulnerableModule {

    static {
        System.loadLibrary("HeapOverflowModule");
    }

    public HeapOverflowModule() {
        super(new HeapOverflowModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        // TODO: Implement! Exploit via enough leaks and breaking crc
        this.output("Scudo Heap Exploitation - Meta Data Buffer Overflow".getBytes());

        // First leak pointer and header
        //this.output(this.leak());

        // Next take input from user. Thus users has to break the checksum themselves.
        byte[] in;

        while (!new String(in = this.input()).equals("EXIT")) {
            this.output(this.processMessage(in));
        }
    }

    private native byte[] leak();

    private native byte[] processMessage(byte[] message);
}
