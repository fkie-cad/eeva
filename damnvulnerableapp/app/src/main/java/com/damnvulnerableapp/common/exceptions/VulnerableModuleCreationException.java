package com.damnvulnerableapp.common.exceptions;

public class VulnerableModuleCreationException extends VulnerableModuleException {

    public VulnerableModuleCreationException(String message) {
        super("Failed to create vulnerable module activity: " + message);
    }

    public VulnerableModuleCreationException(String message, Throwable e) {
        super("Failed to create vulnerable module activity: " + message, e);
    }
}
