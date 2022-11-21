package com.damnvulnerableapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.damnvulnerableapp.helpers.ExternalClient;
import com.damnvulnerableapp.managerservice.ManagerGlobals;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.vulnerable.VulnerableActivity;
import com.damnvulnerableapp.vulnerable.VulnerableGlobals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestVulnerableActivity {

    private static final String PROCESS_NAME = "com.damnvulnerableapp:VulnerableActivity";

    private static final int SETUP_DELAY = 2000;
    private static final int KILL_DELAY = 2000;

    private static final String HOST = VulnerableGlobals.HOST;
    private static final int PORT = VulnerableGlobals.PORT;
    private static final int TIMEOUT = 3000;

    private static final String MODULE_NAME = "StackBufferOverflowModule";
    private static final String DEFAULT_STRING = "test123321TEST";

    private ExternalClient client;

    private ActivityScenario<VulnerableActivity> scenario;

    @Before
    public void setup() throws Exception {
        // All tests share THE SAME ACTIVITY!!! I.e. one client may be able to fully use the app,
        // but switching clients in between takes some time!
        final Intent intent = new Intent(ApplicationProvider.getApplicationContext(), VulnerableActivity.class);
        intent.putExtra(ManagerGlobals.FACTORY_INTENT_KEY, NetworkFactory.class.getSimpleName());
        intent.putExtra(ManagerGlobals.MODULE_INTENT_KEY, MODULE_NAME);

        new Thread(() -> this.scenario = ActivityScenario.launch(intent)).start();

        Thread.sleep(SETUP_DELAY);

        this.client = new ExternalClient(HOST, PORT, TIMEOUT);
    }

    @After
    public void close() {

        // Kill remainders of app...this is REALLY bad style, but whelp...
        int pid = this.getPidByProcessName(PROCESS_NAME);
        if (pid != -1) {
            Process.killProcess(pid);
            while (this.isProcessAlive(pid));
        }

        if (this.client != null)
            this.client.disconnect();

        if (this.scenario != null)
            this.scenario.close();
    }

    @Test
    public void When_SimpleConnecting_Expect_Success() {

        assertTrue(this.client.isConnected());
        this.client.disconnect();
        assertFalse(this.client.isConnected());
    }

    @Test
    public void When_FinishingModuleExit_Expect_Success() throws Exception {

        final PlainMessage initialMessage = this.client.select(MODULE_NAME);
        int pid = Integer.parseInt(new String(initialMessage.getParameters().get(Parameter.CONTENT)));
        assertEquals(Operation.SUCCESS, initialMessage.getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertEquals(Operation.SUCCESS, this.client.forward("EXIT").getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertTrue(this.client.isConnected());
        this.client.exit();

        Thread.sleep(KILL_DELAY);
        assertFalse(this.isProcessAlive(pid));
    }

    @Test
    public void When_FetchingEmpty_Expect_TimeoutException() throws Exception {

        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());

        try {
            this.client.fetch();
        } catch (TimeoutException e) {
            return;
        }
        fail();
    }

    @Test
    public void When_InstantExit_Expect_Success() throws Exception {

        final PlainMessage initialMessage = this.client.select(MODULE_NAME);
        int pid = Integer.parseInt(new String(initialMessage.getParameters().get(Parameter.CONTENT)));
        assertEquals(Operation.SUCCESS, initialMessage.getOperation());
        this.client.exit();

        Thread.sleep(KILL_DELAY);
        assertFalse(this.isProcessAlive(pid));
    }

    @Test
    public void When_DoubleSelect_Expect_Invalid() throws Exception {

        final PlainMessage message = this.client.select(MODULE_NAME);
        int pid = Integer.parseInt(new String(message.getParameters().get(Parameter.CONTENT)));
        assertEquals(Operation.SUCCESS, message.getOperation());
        assertEquals(Operation.INVALID, this.client.select(MODULE_NAME).getOperation());

        this.client.exit();

        Thread.sleep(KILL_DELAY);
        assertFalse(this.isProcessAlive(pid));
    }

    @Test
    public void When_InstantForward_Expect_Invalid() throws Exception {

        final PlainMessage message = this.client.forward(DEFAULT_STRING);
        Log.e(this.getClass().getSimpleName(), message.toString());
        assertEquals(Operation.INVALID, message.getOperation());
        this.client.exit();
    }

    @Test
    public void When_InstantFetch_Expect_Invalid() throws Exception {

        assertEquals(Operation.INVALID, this.client.fetch().getOperation());
        this.client.exit();
    }

    private boolean isProcessAlive(int pid) {

        final ActivityManager manager = (ActivityManager) ApplicationProvider.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procs = manager.getRunningAppProcesses();
        if (procs != null) {
            for (final ActivityManager.RunningAppProcessInfo info : procs) {
                if (info.pid == pid)
                    return true;
            }
        }

        return false;
    }

    private int getPidByProcessName(String processName) {

        final ActivityManager manager = (ActivityManager) ApplicationProvider.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procs = manager.getRunningAppProcesses();
        if (procs != null) {
            for (final ActivityManager.RunningAppProcessInfo info : procs) {
                if (info.processName.equals(processName))
                    return info.pid;
            }
        }
        return -1;
    }
}
