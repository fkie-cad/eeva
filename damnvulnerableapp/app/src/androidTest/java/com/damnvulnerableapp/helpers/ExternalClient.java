package com.damnvulnerableapp.helpers;

import com.damnvulnerableapp.common.configuration.ClientExitConfiguration;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.client.Client;
import com.damnvulnerableapp.networking.communication.client.NetworkConnectionInformation;
import com.damnvulnerableapp.networking.messages.Operation;
import com.damnvulnerableapp.networking.messages.PlainMessage;
import com.damnvulnerableapp.networking.protocol.PlainProtocolFactory;

/**
 * Used for testing CORRECT communication, i.e. no unexpected disconnects, double receives etc.
 * */
public class ExternalClient {

    private final Client client;

    public ExternalClient(String host, int port, int timeout) throws Exception {

        final CommunicationFactory factory = new NetworkFactory();
        factory.setProtocolFactory(new PlainProtocolFactory());
        this.client = factory.createClient();
        this.client.connect(new NetworkConnectionInformation(host, port, timeout));
    }

    public void disconnect() {
        this.client.setConfiguration(new ClientExitConfiguration());
        this.client.disconnect();
    }

    public PlainMessage select(String moduleName) throws Exception {

        this.client.send(new PlainMessage(
                Operation.SELECT,
                moduleName
        ));
        return (PlainMessage) this.client.receive();
    }

    public void exit() throws Exception {

        this.client.send(new PlainMessage(
                Operation.EXIT,
                ""
        ));
        //return (PlainMessage) this.client.receive();
    }

    public PlainMessage exitReceive() throws Exception {

        this.client.send(new PlainMessage(
                Operation.EXIT,
                ""
        ));
        return (PlainMessage) this.client.receive();
    }

    public PlainMessage shutdown() throws Exception {

        this.client.send(new PlainMessage(
                Operation.SHUTDOWN,
                ""
        ));
        return (PlainMessage) this.client.receive();
    }

    public PlainMessage forward(String content) throws Exception {

        this.client.send(new PlainMessage(
                Operation.FORWARD,
                content
        ));
        return (PlainMessage) this.client.receive();
    }

    public PlainMessage fetch() throws Exception {

        this.client.send(new PlainMessage(
                Operation.FETCH,
                ""
        ));
        return (PlainMessage) this.client.receive();
    }

    public boolean isConnected() {
        return this.client.isConnected();
    }
}
