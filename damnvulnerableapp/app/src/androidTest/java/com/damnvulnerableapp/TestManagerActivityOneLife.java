package com.damnvulnerableapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.damnvulnerableapp.helpers.ExternalClient;
import com.damnvulnerableapp.managerservice.ManagerActivity;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.Parameter;
import com.damnvulnerableapp.networking.messages.PlainMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * App is started once. Client connection/disconnect per test case. Expected: no app crashes! ONE LIFE
 * */
@RunWith(AndroidJUnit4.class)
public class TestManagerActivityOneLife {

    private static final int SETUP_DELAY = 2000;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final int TIMEOUT = 3000;

    private static final String MODULE_NAME = "StackBufferOverflowModule";
    private static final String DEFAULT_STRING = "test123321TEST";

    private static final int AMOUNT_REOPENS = 10;

    private ExternalClient client;

    @Rule
    public ActivityScenarioRule<ManagerActivity> activityScenarioRule = new ActivityScenarioRule<>(ManagerActivity.class);

    @Before
    public void setup() throws Exception {
        // All tests share THE SAME ACTIVITY!!! I.e. one client may be able to fully use the app,
        // but switching clients in between takes some time!
        Thread.sleep(SETUP_DELAY);

        this.client = new ExternalClient(HOST, PORT, TIMEOUT);
    }

    @After
    public void close() {

        if (this.client != null)
            this.client.disconnect();
    }

    @Test
    public void When_RunningModule_Expect_Success() throws Exception {

        assertTrue(this.client.isConnected());
        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertEquals(Operation.SUCCESS, this.client.forward(DEFAULT_STRING).getOperation());

        final PlainMessage modified = this.client.fetch();
        assertEquals(Operation.SUCCESS, modified.getOperation());
        assertArrayEquals(DEFAULT_STRING.toUpperCase().getBytes(), modified.getParameters().get(Parameter.CONTENT)); // <- what if module changes?

        assertEquals(Operation.SUCCESS, this.client.exitReceive().getOperation());
    }

    @Test
    public void When_SimpleConnect_Expect_Success() {

        assertTrue(this.client.isConnected());
        this.client.disconnect();
        assertFalse(this.client.isConnected());
    }

    @Test
    public void When_SimpleConnectSelect_Expect_Success() throws Exception {

        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
    }

    @Test
    public void When_SimpleConnectExit_Expect_Invalid() throws Exception {

        assertEquals(Operation.INVALID, this.client.exitReceive().getOperation());
    }

    @Test
    public void When_SimpleConnectForward_Expect_Invalid() throws Exception {

        assertEquals(Operation.INVALID, this.client.forward(DEFAULT_STRING).getOperation());
    }

    @Test
    public void When_SimpleConnectFetch_Expect_Invalid() throws Exception {
        assertEquals(Operation.INVALID, this.client.fetch().getOperation());
    }

    @Test
    public void When_DoubleSelect_Expect_Invalid() throws Exception {

        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
        assertEquals(Operation.INVALID, this.client.select(MODULE_NAME).getOperation());
    }

    // NOTE: Specific to vulnerable module!
    @Test
    public void When_SelectedDoubleFetch_Expect_TimeoutException() throws Exception {

        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());

        try {
            this.client.fetch();
        } catch (TimeoutException e) {
            return;
        }
        fail();
    }

    // NOTE: Specific to vulnerable module!
    @Test
    public void When_SelectedMultipleForwardFetch_Expect_Success() throws Exception {

        //assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
        Log.i(this.getClass().getSimpleName(), this.client.select(MODULE_NAME).toString());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());

        assertEquals(Operation.SUCCESS, this.client.forward(DEFAULT_STRING).getOperation());
        assertEquals(Operation.SUCCESS, this.client.forward(DEFAULT_STRING).getOperation());
        assertEquals(Operation.SUCCESS, this.client.forward(DEFAULT_STRING).getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
        assertEquals(Operation.SUCCESS, this.client.fetch().getOperation());
    }

    @Test
    public void When_MultipleSelectExit_Expect_Success() throws Exception {

        for (int i = 0; i < AMOUNT_REOPENS; i++) {
            assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());
            assertEquals(Operation.SUCCESS, this.client.exitReceive().getOperation());
        }
    }

    @Test
    public void When_TwoClientsConnect_Expect_Success() throws Exception {

        final ExternalClient second = new ExternalClient(HOST, PORT, TIMEOUT);
        assertTrue(this.client.isConnected());

        assertNull(second.select(MODULE_NAME));
        assertEquals(Operation.SUCCESS, this.client.select(MODULE_NAME).getOperation());

        second.disconnect();
    }
}
