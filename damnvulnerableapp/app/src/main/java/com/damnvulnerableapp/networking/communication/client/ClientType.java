package com.damnvulnerableapp.networking.communication.client;

/**
 * Type/identifier of a client. This is useful in settings where a client has to tell a server
 * about its functionality/task.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public enum ClientType {
    /**
     * Indicates that a client is a (real) user, i.e. an uncontrollable entity, from which to
     * expect further instructions.
     * */
    USER("USER"),

    /**
     * Indicates that a client is a command handler, i.e. it is responsible to forward commands
     * between internal components to e.g. ensure a graceful application shutdown or resource close.
     * */
    COMMAND("COMMAND"),

    /**
     * Indicates that a client is representing the I/O of a program, i.e. its input component will
     * be forwarded to an internal program, whereas its output component will be fetched and
     * forwarded to whoever created this client.
     * */
    IO("IO"),

    /**
     * Indicates that a client is constructed by a {@link com.damnvulnerableapp.networking.communication.server.Server}.
     * Therefore it is just responsible for handling events via {@link CommunicationListener}. It
     * is not allowed to connect two clients of this type.
     * */
    MANAGER("MANAGER");

    /**
     * Enum value.
     * */
    private final String type;

    /**
     * Set enum value.
     *
     * @param type Value of the enum indicating a client type.
     * */
    ClientType(String type) {this.type = type;}

    /**
     * Converts enum value to a string.
     *
     * @return String value of the enum.
     * */
    @Override
    public String toString() {return this.type;}
}
