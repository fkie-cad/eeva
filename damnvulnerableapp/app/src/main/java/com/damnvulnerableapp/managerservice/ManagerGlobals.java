package com.damnvulnerableapp.managerservice;

/**
 * Data class that contains all globals that need to be accessible from different modules.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ManagerGlobals {

    /**
     * Key of the factory name that is passed to vulnerable activity via intents.
     * */
    public static final String FACTORY_INTENT_KEY = "FACTORY";

    /**
     * Key of the module name to load. This is passed to the vulnerable activity via intents.
     * */
    public static final String MODULE_INTENT_KEY = "MODULE";
}
