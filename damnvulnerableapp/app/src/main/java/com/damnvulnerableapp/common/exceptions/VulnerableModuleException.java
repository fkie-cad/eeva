package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if any error related to the vulnerable module occurs.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see VulnerableModuleCommunicationException
 * */
public class VulnerableModuleException extends Exception {

    public VulnerableModuleException(String message) {
        super("Error in vulnerable module: " + message);
    }

    public  VulnerableModuleException(String message, Throwable e) {
        super("Error in vulnerable module: " + message, e);
    }
}
