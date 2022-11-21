package com.damnvulnerableapp.common.exceptions;

/**
 * Exception that will be thrown, if communicating with the selected, vulnerable module fails. This
 * is often a wrapper for {@link com.damnvulnerableapp.networking.exceptions.ConnectionException}s.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class VulnerableModuleCommunicationException extends VulnerableModuleException {

    public VulnerableModuleCommunicationException(String message) {
        super("Failed to communicate with vulnerable module: " + message);
    }

    public VulnerableModuleCommunicationException(String message, Throwable e) {
        super("Failed to communicate with vulnerable module: " + message, e);
    }
}
