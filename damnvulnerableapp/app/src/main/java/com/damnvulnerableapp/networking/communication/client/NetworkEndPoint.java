package com.damnvulnerableapp.networking.communication.client;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.networking.communication.NetworkFactory;
import com.damnvulnerableapp.networking.communication.server.Server;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.InternalSocketException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Socket - based communication endpoint.
 *
 * This is a concrete implementation of {@link EndPoint} and therefore represents one peer in
 * peer - to - peer communication. Internally, {@link Socket} is used in conjunction with
 * {@link DataInputStream} and {@link DataOutputStream}.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public class NetworkEndPoint implements EndPoint { //extends EndPoint {

    /**
     * Socket that is used to connect and communication with a server, i.e. {@link java.net.ServerSocket}.
     * */
    private final Socket socket;

    /**
     * Input portion of I/O. It will be used to receive messages from a {@link com.damnvulnerableapp.networking.communication.server.Server}.
     * */
    private DataInputStream input;

    /**
     * Output portion of I/O. It will be used to send messages to a {@link com.damnvulnerableapp.networking.communication.server.Server}.
     * */
    private DataOutputStream output;

    /**
     * Construct new endpoint based on a completely new socket. This will be used in {@link NetworkFactory#createClient()}
     * as its internal socket is NOT coming from a {@link Server#accept()}.
     *
     * @throws ConnectionException If, given a socket returned by {@link ServerSocket#accept()},
     *                             constructing I/O streams fails. Should not affect this constructor.
     */
    public NetworkEndPoint() throws ConnectionException {
        this(new Socket());
    }

    /**
     * Construct new endpoint based on a socket returned by {@link ServerSocket#accept()}. If the
     * socket is already connected, then I/O streams will be created as well.
     *
     * @param socket Socket to attach to this endpoint. This must not be null.
     * @throws ConnectionException If, given a socket returned by {@link ServerSocket#accept()},
     *                             constructing I/O streams fails.
     * */
    public NetworkEndPoint(Socket socket) throws ConnectionException {

        this.socket = socket;
        if (this.socket.isConnected())
            this.initStreams();
    }

    /**
     * Connects this endpoint to specified server. To that end, {@link Socket#connect(SocketAddress, int)}
     * is used with a timeout specified in currently selected configuration. This method will also
     * set a timeout for receiving messages via {@link Socket#setSoTimeout(int)}.
     *
     * It is not allowed to connect a socket - based endpoint multiple times. Just like with
     * {@link Socket}s, a new endpoint has to be created.
     *
     * It is possible to connect to a {@link ServerSocket} successfully, before {@link ServerSocket#accept()}
     * is called. {@link ServerSocket#accept()} just constructs a socket based on a recent
     * connection located on the backlog.
     *
     * @param info Information on where to connect to.
     * @throws ConnectionException If an endpoint is already connected or some other connection
     *                             related error occurs.
     * @throws TimeoutException Connecting to a server may timeout, e.g. if the server is not
     *                          available or does not answer.
     * */
    @Override
    public void connect(ConnectionInformation info) throws ConnectionException, TimeoutException {

        NetworkConnectionInformation target = (NetworkConnectionInformation) info;

        // If socket is already connected, we cannot just reconnect as this requires a new socket
        // to be created.
        if (this.socket.isConnected())
            throw new ConnectionException("Socket is already connected.");

        // Connect to target host
        try {
            this.socket.connect(
                    new InetSocketAddress(
                            target.getHost(),
                            target.getPort()
                    ),
                    info.getTimeout()
            );
        } catch (SocketTimeoutException e) {
            throw new TimeoutException("Timed out while trying to connect to " +
                    target.getHost() + ":" + target.getPort() + ".", e);
        } catch (IOException e) {
            throw new ConnectionException("Connecting to " + target.getHost() + ":"
                    + target.getPort() + " failed.", e);
        }

        this.initStreams();
    }

    /**
     * Disconnects this endpoint from an established connection by closing this {@link Socket}
     * and corresponding I/O streams. Any exceptions that may occur during closing are ignored, as
     * they do not yield useful information in this case.
     */
    @Override
    public void disconnect() {

        // Shutdown I/O and socket. Why split this step: If output.close threw an exception
        // then we would not close the socket -> separate tries
        try {
            if (this.output != null)
                this.output.close();
        } catch (IOException ignored) {}

        try {
            if (this.input != null)
                this.input.close();
        } catch (IOException ignored) {}

        try {
            if (this.socket != null)
                this.socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Returns whether this endpoint is connected to a {@link Server}.
     *
     * This method is based on {@link Socket#isConnected()}, which will be set to <code>true</code>,
     * if this {@link Socket} connected successfully. Unfortunately, {@link Socket#isConnected()}
     * will never be <code>false</code> again. Therefore, for a socket to be connected, it has to
     * be in connected state and not closed.
     *
     * @return <code>true</code>, if socket is not closed and connected; <code>false</code> otherwise.
     * */
    @Override
    public boolean isConnected() {
        return (!this.socket.isClosed() && this.socket.isConnected());
    }

    /**
     * Sends a raw message via a {@link DataOutputStream}. After the message has been sent according
     * to the format described in {@link EndPoint#send(byte[])}, this {@link DataOutputStream} will
     * be flushed to ensure that the message is sent.
     *
     * If a message was dropped, this might make this endpoint stumble, as it does not take any
     * measures to recover from missing messages.
     * 
     * @param message Array of bytes to send to other peer. If it is <code>null</code>, then it will
     *                be set to an empty byte array.
     * @throws ConnectionException If the connection is shut down during sending or has already
     *                             been shut down before sending.
     * @see EndPoint#send(byte[])
     * */
    @Override
    public void send(byte[] message) throws ConnectionException {

        try {

            // If message is null, set it to empty
            if (message == null)
                message = new byte[0];

            // First send length of message to send
            this.output.writeInt(message.length);

            // Write whole message
            this.output.write(message, 0, message.length);

            // Enforce write
            this.output.flush();

        } catch (IOException e) {
            // We do not care about SocketTimeoutException, because, according to docs, this can
            // only occur "... on a socket read or accept".
            throw new ConnectionException("Connection closed. Failed to send message.", e);
        }
    }

    /**
     * Receives a raw message using a {@link DataInputStream} in a blocking fashion. If the message
     * received is of length 0, then an empty byte array is returned.
     *
     * @return Raw message.
     * @throws ConnectionException If connection is shut down while or before waiting for a message.
     * @throws TimeoutException As receiving a message blocks until there is a message, this may
     *                          timeout. Therefore this is thrown on receive timeout.
     * @see EndPoint#receive()
     * */
    @Override
    @NonNull
    public byte[] receive() throws TimeoutException, ConnectionException {

        byte[] message;

        try {

            // Read message length
            int length = this.input.readInt();
            if (length > 0) {
                message = new byte[length];

                // Read whole message based on length.
                this.input.readFully(message, 0, length);
            } else {
                message = new byte[0];
            }

            return message;
        } catch (SocketTimeoutException e) {
            throw new TimeoutException("Timed out while trying to receive a message.", e);
        } catch (IOException e) {
            throw new ConnectionException("Connection closed. Failed to receive message.", e);
        }
    }

    @Override
    public void setTimeout(int timeout) throws InternalSocketException {
        try {
            this.socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            throw new InternalSocketException("Trying to set timeout on server socket failed.", e);
        }
    }

    @Override
    public int getTimeout() throws InternalSocketException {
        try {
            return this.socket.getSoTimeout();
        } catch (SocketException e) {
            throw new InternalSocketException("Trying to set timeout on server socket failed.", e);
        }
    }

    /**
     * Initializes input and output streams of this socket. Any error is vital, thus deadly
     * exceptions are thrown on error.
     *
     * @throws ConnectionException If creating an I/O stream fails. This can be due to a closed
     *                             connection, not connected socket, a closed stream etc.
     * @see DataInputStream
     * @see DataOutputStream
     * */
    private void initStreams() throws ConnectionException {
        // Create input stream
        try {
            this.input = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            throw new ConnectionException("Failed to create input stream. Either"
                    + " stream creation failed, the connection was closed, the socket was"
                    + " not connected or the respective stream was closed.", e);
        }

        // Create output stream
        try {
            this.output = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            try {
                // Try to be nice and clean up opened input stream to avoid resource leaks.
                this.input.close();
            } catch (IOException ignored) {}
            throw new ConnectionException("Failed to create output stream. Either"
                    + " stream creation failed, the connection was closed, the socket was"
                    + " not connected or the respective stream was closed.", e);
        }
    }
}
