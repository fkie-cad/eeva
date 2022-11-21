package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class OffByOneModule extends VulnerableModule {

    static {
        System.loadLibrary("OffByOneModule");
    }

    public OffByOneModule() {
        super(new OffByOneModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        this.output("Welcome to the most secure message logger in the world!".getBytes());

        byte[] message;
        while (true) {

            this.output("Enter a message to log: ".getBytes());
            message = this.input();
            if (message != null) {
                if (new String(message).equals("EXIT"))
                    break;

                message = OffByOneModule.logMessage(message);
                this.output(message);
            } else {
                this.output("Failed to receive the message to log...Better safe than sorry!".getBytes());
            }
        }

        this.output("Your logged message(s) were stored successfully.".getBytes());
    }

    private native static byte[] logMessage(byte[] message);
}
