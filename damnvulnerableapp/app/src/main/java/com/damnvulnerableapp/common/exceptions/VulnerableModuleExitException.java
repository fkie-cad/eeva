package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if the vulnerable module exited by itself.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class VulnerableModuleExitException extends VulnerableModuleException {

    public VulnerableModuleExitException(String message) {
        super("Vulnerable module quit: " + message);
    }

    public VulnerableModuleExitException(String message, Throwable e) {
        super("Vulnerable module quit: " + message, e);
    }
}
