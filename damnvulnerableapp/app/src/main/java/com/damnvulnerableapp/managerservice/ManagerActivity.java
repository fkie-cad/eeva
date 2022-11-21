package com.damnvulnerableapp.managerservice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.damnvulnerableapp.managerservice.ManagerService;

/**
 * Initial activity that kicks off the whole application. It basically just starts a background
 * service that does all the heavy lifting.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class ManagerActivity extends AppCompatActivity {

    /**
     * Requests permission required in order to avoid the service timing out after a while (10s).
     * Afterwards, it starts the service. If the permission is not granted, then the app will w.h.p.
     * NOT work properly.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.canDrawOverlays(this)) {
            final Intent overlayPermissions = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            this.startActivity(overlayPermissions);
        }

        final Intent service = new Intent(this, ManagerService.class);
        this.startService(service);
        this.finish();
    }
}