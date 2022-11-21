package com.damnvulnerableapp.managerservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.damnvulnerableapp.common.exceptions.InitializationException;
import com.damnvulnerableapp.managerservice.views.ExternalView;
import com.damnvulnerableapp.networking.exceptions.BindException;
import com.damnvulnerableapp.networking.exceptions.CreationException;
import com.damnvulnerableapp.networking.exceptions.InternalSocketException;

/**
 * Background service that will listen for incoming connections. It will handle all management -
 * related work like spawning vulnerable activities, handling I/O etc.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ManagerService extends Service {

    /**
     * Sole instance of this class.
     * */
    private static ManagerService instance;

    /**
     * Returns sole instance of this class.
     *
     * @return Sole instance.
     * */
    public static ManagerService getInstance() {
        return ManagerService.instance;
    }

    /**
     * Creates and sets up the {@link com.damnvulnerableapp.networking.communication.server.Server}
     * used for communication with external clients. It also implicitly creates {@link com.damnvulnerableapp.managerservice.controllers.ExternalController}
     * that manages the app's logic.
     * */
    @Override
    public final int onStartCommand(Intent intent, int flags, int startID) {
        ManagerService.instance = this;

        // Create external view
        final ExternalView externalView = new ExternalView();
        try {
            externalView.init();
        } catch (BindException | InternalSocketException | CreationException | InitializationException e) {
            // Exit if view cannot be created.
            this.stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public final void onDestroy() {}

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        return null;
    }
}
