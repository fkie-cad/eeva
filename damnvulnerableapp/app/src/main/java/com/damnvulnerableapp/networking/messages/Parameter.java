package com.damnvulnerableapp.networking.messages;

/**
 * Identifier for a parameter in a {@link PlainMessage}. Such an identifier always precedes its
 * corresponding value. Thus this is like the "key" - portion of a key - value - pair and the value
 * of a parameter is the "value" - portion.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * @see PlainMessage
 * */
public enum Parameter {

    /**
     * Indicates that the value following this parameter is content that e.g. contains a byte
     * array to be forwarded to a running module.
     *
     * This parameter is the ONLY mandatory parameter (so far).
     * */
    CONTENT("CONTENT"),

    /**
     * Indicates that the value following this parameter contains information.
     * */
    INFO("INFO");

    /**
     * Enum value.
     * */
    private final String parameter;

    /**
     * Set enum value.
     *
     * @param parameter Parameter of a {@link PlainMessage}.
     * */
    Parameter(String parameter) {this.parameter = parameter;}

    /**
     * Converts enum value to a string.
     *
     * @return String value of the enum.
     * */
    @Override
    public String toString() {return this.parameter;}
}
