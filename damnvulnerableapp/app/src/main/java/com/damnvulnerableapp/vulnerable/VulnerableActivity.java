package com.damnvulnerableapp.vulnerable;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.damnvulnerableapp.common.DynamicClassLoader;
import com.damnvulnerableapp.common.exceptions.MVCException;
import com.damnvulnerableapp.managerservice.ManagerGlobals;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;
import com.damnvulnerableapp.vulnerable.view.InternalView;

/**
 * Activity of vulnerable module. Every vulnerable module will be outsourced into a separate process
 * running a separate activity.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class VulnerableActivity extends AppCompatActivity {

    /**
     * Sole instance of this class.
     * */
    private static VulnerableActivity instance;

    /**
     * Returns sole instance of this class.
     *
     * @return Sole instance.
     * */
    public static VulnerableActivity getInstance() {
        return VulnerableActivity.instance;
    }

    /**
     * Called upon app creation. It first checks for necessary intents from manager, i.e. the name
     * of the {@link CommunicationFactory} used by {@link com.damnvulnerableapp.managerservice.controllers.states.VulnerableAppState}
     * and the name of the module to load. If any of required intents is missing, this app will shut
     * down. Then this method will try to load the {@link CommunicationFactory}. If this fails, this
     * app will be shut down. Finally, it will try to setup communication by initializing the
     * {@link InternalView}. Again, if that fails, this app will be shut down.
     *
     * @param ignored Ignored.
     * */
    @Override
    protected final void onCreate(Bundle ignored) {
        super.onCreate(ignored);

        VulnerableActivity.instance = this;

        // 1. Get intents:
        // - factory name
        // - module name
        final Intent intent = this.getIntent();
        if (!intent.hasExtra(ManagerGlobals.FACTORY_INTENT_KEY) || !intent.hasExtra(ManagerGlobals.MODULE_INTENT_KEY))
            this.finishAndRemoveTask();

        final String factoryName = intent.getStringExtra(ManagerGlobals.FACTORY_INTENT_KEY);
        final String moduleName = intent.getStringExtra(ManagerGlobals.MODULE_INTENT_KEY);

        // Load factory
        final CommunicationFactory factory = (CommunicationFactory) DynamicClassLoader.getInstance().loadClass(
                CommunicationFactory.class.getPackage(),
                factoryName,
                CommunicationFactory.class
        );
        if (factory == null) {
            this.finishAndRemoveTask();
            return;
        }
        factory.setProtocolFactory(new PlainProtocolFactory());

        // 2. set up internal view
        final InternalView internalView = new InternalView(factory);
        try {
            internalView.init(moduleName);
        } catch (CommunicationException | MVCException e) {
            this.finishAndRemoveTask();
        }
    }
}