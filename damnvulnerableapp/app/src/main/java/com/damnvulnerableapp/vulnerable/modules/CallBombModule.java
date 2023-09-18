package com.damnvulnerableapp.vulnerable.modules;

import com.damnvulnerableapp.common.exceptions.VulnerableModuleException;

public class CallBombModule extends VulnerableModule {

    static {
        System.loadLibrary("CallBombModule");
    }

    public CallBombModule() {
        super(new CallBombModuleConfiguration());
    }

    @Override
    public void main() throws VulnerableModuleException {

        this.output("Call Bomb incoming...".getBytes());
        while (!new String(this.input()).equals("EXIT")) {
            CallBombModule.callBomb();
            this.output("Result".getBytes());
        }
    }

    public static native void callBomb();
}
