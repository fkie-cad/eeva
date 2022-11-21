package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if e.g. fetching data from the vulnerable module or forwarding
 * data to the vulnerable module fails
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class VulnerableModuleOperationException extends VulnerableModuleException {

    public VulnerableModuleOperationException(String message) {
        super("Operation not successful: " + message);
    }

    public VulnerableModuleOperationException(String message, Throwable e) {
        super("Operation not successful: " + message, e);
    }
}
