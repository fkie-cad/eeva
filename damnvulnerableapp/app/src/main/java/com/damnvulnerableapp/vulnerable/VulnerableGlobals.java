package com.damnvulnerableapp.vulnerable;

/**
 * Data class that wraps all globals of the vulnerable activity that need to be accessible in other
 * modules as well.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class VulnerableGlobals {

    /**
     * Address and port, on which to bind the internal server.
     * */
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 8081;

    /**
     * Timeout for external client. Defaults to 10s. It is expected that the vulnerable activity
     * is up and running within 10s. Otherwise the app may as well crash.
     * */
    public static final int CONNECT_TIMEOUT = 10000;
}
