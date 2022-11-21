package com.damnvulnerableapp;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.damnvulnerableapp.helpers.RawExternalClient;
import com.damnvulnerableapp.managerservice.ManagerActivity;
import com.damnvulnerableapp.networking.messages.Operation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestManagerActivityOneLifeRaw {

    private static final int SETUP_DELAY = 1000;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final int TIMEOUT = 3000;

    private static final String MODULE_NAME = "StackBufferOverflowModule";

    private RawExternalClient client;

    @Rule
    public ActivityScenarioRule<ManagerActivity> activityScenarioRule = new ActivityScenarioRule<>(ManagerActivity.class);

    @Before
    public void setup() throws Exception {
        // All tests share THE SAME ACTIVITY!!!
        Thread.sleep(SETUP_DELAY);

        this.client = new RawExternalClient(HOST, PORT, TIMEOUT);
    }

    @After
    public void close() throws Exception {
        if (this.client != null)
            this.client.disconnect();
    }

    @Test
    public void When_UnknownOperation_Expect_Invalid() throws Exception {

        client.send("CONTENT " + "SLCT" + " CONTENT " + MODULE_NAME);
        assertTrue(client.receive().contains(Operation.INVALID.toString()));
    }

    @Test
    public void When_SelectedUnknownOperation_Expect_Invalid() throws Exception {

        client.send("CONTENT SELECT CONTENT " + MODULE_NAME);
        assertTrue(client.receive().contains(Operation.SUCCESS.toString()));
        client.send("CONTENT " + "SLCT" + " CONTENT " + MODULE_NAME);
        assertTrue(client.receive().contains(Operation.INVALID.toString()));
    }
}
