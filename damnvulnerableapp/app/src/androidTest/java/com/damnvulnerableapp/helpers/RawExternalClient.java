package com.damnvulnerableapp.helpers;

import com.damnvulnerableapp.networking.communication.client.ClientType;
import com.damnvulnerableapp.networking.communication.client.NetworkEndPoint;
import com.damnvulnerableapp.networking.protocol.PlainClientProtocol;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class RawExternalClient {

    private final NetworkEndPoint endpoint;
    private final PlainClientProtocol protocol;

    public RawExternalClient(String host, int port, int timeout) throws Exception {

        final Socket socket = new Socket();
        socket.setSoTimeout(timeout);
        socket.connect(new InetSocketAddress(host, port), timeout);
        this.endpoint = new NetworkEndPoint(socket);

        this.protocol = new PlainClientProtocol();
        this.protocol.handshake(this.endpoint, ClientType.USER);
    }

    public boolean isConnected() {
        return this.endpoint.isConnected();
    }

    public void disconnect() throws Exception {

        this.protocol.shutdown(this.endpoint);
        this.endpoint.disconnect();
    }

    public void send(String message) throws Exception {
        this.endpoint.send(message.getBytes());
    }

    public String receive() throws Exception {
        return new String(this.endpoint.receive());
    }
}
