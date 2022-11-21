package com.damnvulnerableapp.networking.communication.client;

import androidx.annotation.NonNull;

import com.damnvulnerableapp.common.configuration.ClientConfiguration;
import com.damnvulnerableapp.common.configuration.Configurable;
import com.damnvulnerableapp.common.configuration.Configuration;
import com.damnvulnerableapp.networking.communication.CommunicationFactory;
import com.damnvulnerableapp.networking.exceptions.CommunicationException;
import com.damnvulnerableapp.networking.exceptions.ConnectionException;
import com.damnvulnerableapp.networking.exceptions.InvalidClientException;
import com.damnvulnerableapp.networking.exceptions.MessageParserException;
import com.damnvulnerableapp.networking.exceptions.TimeoutException;
import com.damnvulnerableapp.networking.messages.Message;
import com.damnvulnerableapp.networking.protocol.Protocol;

import java.util.LinkedList;
import java.util.List;

/**
 * A client that can be used to send requests to a {@link com.damnvulnerableapp.networking.communication.server.Server}.
 * It is by default independent of any communication - specific implementations, i.e. one may
 * implement communication via file mappings, socket etc. This client is a wrapper for those
 * implementations and provides an easy way to handle messages.
 *
 * @author Pascal Kühnemann
 * @see com.damnvulnerableapp.networking.communication.server.Server
 * @see Configurable
 * */
public abstract class Client implements Configurable {

    /**
     * Type or identifier of the client. This is useful when connecting to a server that cares
     * about the functionality of a client. E.g. a client could be a "normal" user, an automated
     * command handler etc. Defaults to {@link ClientType#USER}.
     * @see ClientType
     * */
    private ClientType type;

    /**
     * Wrapping communication layer that lies between the low - level implementation of
     * communication and this client. It determines how messages are sent, how to handle initial
     * connects and how to shutdown a connection.
     * @see Protocol
     * */
    private Protocol protocol;

    /**
     * Low - level implementation of communication. This may e.g. be a socket - based or file
     * mapping - based implementation. Depending on the setting, on implementation may be more
     * stable than the other.
     * @see EndPoint
     * */
    private EndPoint endpoint;

    /**
     * A list of {@link CommunicationListener} objects that will be called on certain events. Any
     * method that triggers an event will handle calling the correct callbacks. It may be possible
     * that an event is fired multiple times, because or parallelism.
     * @see CommunicationListener
     * */
    private final List<CommunicationListener> listeners;

    /**
     * {@link com.damnvulnerableapp.common.configuration.Configuration} class holding information on
     * low - level - specific details.
     * @see com.damnvulnerableapp.common.configuration.Configuration
     * */
    private ClientConfiguration configuration;

    /**
     * Construct list of {@link CommunicationListener} and this {@link com.damnvulnerableapp.common.configuration.Configuration}.
     *
     * This construct will be called, if a new client is created that actually wants to perform
     * connect operations.
     *
     * Do NOT call any constructor of this class by instantiating a subclass. Rather use
     * {@link CommunicationFactory#createClient()} for creating clients.
     * */
    public Client() {

        this.listeners = new LinkedList<>();

        this.setConfiguration(new ClientConfiguration());

        this.type = ClientType.USER;
    }

    /**
     * Construct this client as in {@link Client#Client()}. Additionally, this fixes a low - level
     * implementation that is already created, i.e. this constructor will be used by a
     * {@link com.damnvulnerableapp.networking.communication.server.Server} when accepting incoming
     * client connections.
     *
     * Do NOT call any constructor of this class by instantiating a subclass. Rather use
     * {@link CommunicationFactory#createClient()} for creating clients.
     *
     * @param endpoint Low - level implementation constructed by {@link com.damnvulnerableapp.networking.communication.server.Server}
     *                 when accepting incoming connection.
     * */
    public Client(EndPoint endpoint) {
        this();
        this.endpoint = endpoint;
    }

    /**
     * Connects to a given target, described by <code>info</code>. If the low - level implementation
     * is already up and running, this method will omit creating a new instance of {@link EndPoint}
     * and immediately start with a {@link Protocol#handshake(EndPoint, ClientType)}. After the
     * {@link Protocol#handshake(EndPoint, ClientType)}, all {@link CommunicationListener#onConnect(Client)}
     * will be called.
     *
     * A {@link com.damnvulnerableapp.networking.communication.server.Server}, which is accepting
     * incoming client connections, also calls this method, but with an already connected endpoint.
     * Therefore, for server - side clients, only the handshake and listeners are invoked.
     *
     * @param info Information on where and how to connect. This depends on the low - level
     *             implementation of {@link EndPoint}.
     * @throws ConnectionException If communication partner disconnects unexpectedly, the low - level
     *                             implementation unexpectedly fails creating a connection or the
     *                             {@link Protocol#handshake(EndPoint, ClientType)} fails.
     * @throws MessageParserException If parsing a message from {@link Protocol#handshake(EndPoint, ClientType)}
     *                                fails.
     * @throws TimeoutException If connecting to a server or reading exceeds a configured time limit.
     * @see Protocol
     * @see EndPoint
     * */
    public void connect(ConnectionInformation info) throws CommunicationException {

        // If endpoint is not in connected state: connect it
        if (!this.isConnected()) {

            this.endpoint = this.createEndPoint();
            this.setConfiguration(this.configuration);

            final int oldTimeout = this.endpoint.getTimeout();
            this.endpoint.setTimeout(info.getTimeout());
            this.endpoint.connect(info);
            this.endpoint.setTimeout(oldTimeout);
        }

        try {

            final int oldTimeout = this.endpoint.getTimeout();
            this.endpoint.setTimeout(this.configuration.getHandshakeTimeout());
            this.protocol.handshake(this.endpoint, this.type);
            this.endpoint.setTimeout(oldTimeout);
        } catch (InvalidClientException ignored) {}

        for (CommunicationListener listener : this.listeners)
            listener.onConnect(this);
    }

    /**
     * Disconnect this client from established connection. Before shutting down the connection
     * and closing this {@link EndPoint}, all {@link CommunicationListener#onDisconnect(Client)}
     * will be called.
     *
     * Before closing resources used in the internal {@link EndPoint} instance, {@link Protocol#shutdown(EndPoint)}
     * is used to aim for a graceful shutdown. As this method may be called on closed clients
     * as well in order to insure that all internal resources are closed, all exceptions of
     * {@link Protocol#shutdown(EndPoint)} will be discarded.
     *
     * This method may also be called, if the connection is already closed, i.e. to free resources
     * used by internal objects.
     *
     * @see Protocol
     * @see EndPoint
     * */
    public void disconnect() {

        for (CommunicationListener listener : this.listeners)
            listener.onDisconnect(this);

        if (this.isConnected()) {
            try {
                // Hope that gracefully shutting down connections works...
                this.protocol.shutdown(this.endpoint);
            } catch (CommunicationException ignored) {
            }
        }
        if (this.endpoint != null)
            this.endpoint.disconnect();
    }

    /**
     * Checks whether the underlying {@link EndPoint} is still connected to a server.
     *
     * @return <code>true</code>, if {@link EndPoint} is connected; <code>false</code> otherwise.
     * @see EndPoint#isConnected()
     * */
    public boolean isConnected() {
        if (this.endpoint != null)
            return this.endpoint.isConnected();
        return false;
    }

    /**
     * Sends a {@link Message} to a communication partner using a fixed {@link Protocol}. Before
     * sending the message, if this client is still connected, all {@link CommunicationListener#onSend(Client, Message)}
     * will be called.
     *
     * If there is a connection issue, i.e. internally some method throws {@link ConnectionException},
     * then this client will automatically be disconnected.
     *
     * @param message Message to send to the communication partner using a specified protocol.
     *                Setting this to <code>null</code> will result in protocol - dependent handling
     *                of the message.
     * @throws ConnectionException If communication partner disconnects unexpectedly, the low - level
     *                             implementation unexpectedly fails receiving a message or unexpectedly
     *                             fails sending a message. Also the low - level implementation may
     *                             be not connected anymore due to unknown reasons. This client is
     *                             automatically disconnected. Also thrown if client was not connected
     *                             via {@link Client#connect(ConnectionInformation)}.
     * @throws MessageParserException If <code>message</code> is of an invalid format w.r.t. the
     *                                underlying protocol.
     * @throws TimeoutException If sending a message exceeds a preconfigured time limit. This may be
     *                          linked to waiting for ACK messages after a sent message.
     * @throws CommunicationException If any communication - related error occurs. It will enforce
     *                                a disconnect.
     * @see Protocol
     * @see Message
     * @see com.damnvulnerableapp.networking.messages.MessageParser
     * */
    public void send(Message message) throws CommunicationException {

        if (!this.isConnected())
            throw new ConnectionException("Client is not connected.");

        for (CommunicationListener listener : this.listeners)
            listener.onSend(this, message);

        // This will throw an exception on disconnected endpoint.
        try {
            this.protocol.send(this.endpoint, message);
        } catch (CommunicationException e) {

            // Handle disconnect
            this.disconnect();
            throw e;
        }
    }

    /**
     * Receives a message by blocking until there is a message available.
     *
     * If there is a connection issue, i.e. internally some method throws {@link ConnectionException},
     * then this client will automatically be disconnected.
     * 
     * Receiving a message will trigger all {@link CommunicationListener#onReceive(Client, Message)}
     * and thus a client can post requests either by actively using {@link Client#send(Message)} or
     * reactively by calling {@link Client#send(Message)} in {@link CommunicationListener#onReceive(Client, Message)}.
     *
     * E.g. in {@link com.damnvulnerableapp.networking.communication.server.NetworkServer}, there the
     * clients will permanently listen for incoming requests, forward the requests to
     * {@link CommunicationListener#onReceive(Client, Message)}, which in turn may reactively answer
     * the requests with corresponding responses messages sent via {@link Client#send(Message)}.
     *
     * @return Message If a valid message is received, then this will be returned; null otherwise.
     * @throws ConnectionException If communication partner disconnects unexpectedly, the low - level
     *                             implementation unexpectedly fails receiving a message or unexpectedly
     *                             fails sending a message. Also the low - level implementation may
     *                             be not connected anymore due to unknown reasons. This client is
     *                             automatically disconnected. Also thrown if client was not connected
     *                             via {@link Client#connect(ConnectionInformation)}.
     * @throws MessageParserException If a received message is of an invalid format.
     * @throws TimeoutException If waiting for an incoming message exceeds a preconfigured time limit.
     * @throws CommunicationException If any communication - related error occurs.
     * @see Protocol
     * @see Message
     * @see com.damnvulnerableapp.networking.messages.MessageParser
     * */
    public Message receive() throws CommunicationException {

        if (!this.isConnected())
            throw new ConnectionException("Client is not connected.");

        Message message;
        try {
            message = this.protocol.receive(this.endpoint);
        } catch (ConnectionException e) {

            // Handle disconnect
            this.disconnect();
            throw e;
        } catch (MessageParserException e) {

            // Handle invalid messages
            for (CommunicationListener listener : this.listeners)
                listener.onInvalidMessage(this, e.getInvalidMessage());

            throw e;
        }
        if (message == null)
            return null;

        for (CommunicationListener listener : this.listeners)
            listener.onReceive(this, message);

        return message;
    }

    /**
     * Adds a {@link CommunicationListener} to this list of communication listeners. Its callbacks
     * will be invoked on specified event.
     *
     * @param listener Instance of {@link CommunicationListener} that should be invoked on events.
     * @see CommunicationListener
     * */
    public void addCommunicationListener(CommunicationListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a {@link CommunicationListener} from this list of registered listeners. Therefore
     * its callbacks will no longer be invoked on occurring events.
     *
     * @param listener Listener to remove.
     * @see CommunicationListener
     * */
    public void removeCommunicationListener(CommunicationListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Specifies this {@link Protocol} to be used for communication with a server.
     *
     * Do NOT call this method. It is used in {@link CommunicationFactory} for linking a protocol
     * to clients and servers. Calling this method may result in connection crashes etc.
     *
     * @param protocol {@link Protocol} to use for communication.
     * @see Protocol
     * */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Specifies this {@link ClientType} that identifies a client's functionality.
     *
     * @param type Identifier for client's functionality.
     * @see ClientType
     * */
    public void setType(ClientType type) {
        this.type = type;
    }

    /**
     * Provides a {@link ClientConfiguration} to be used for various configurable aspects of this
     * client. Primarily, this will have an effect on this internal {@link EndPoint}. This will
     * overwrite the default configuration.
     *
     * This method should be called before calling any other method of {@link Client}.
     *
     * @param configuration New configuration to use.
     * @see ClientConfiguration
     * @see Configurable
     * */
    @Override
    public void setConfiguration(Configuration configuration) {
        // Check if configuration is a subclass of EndPointConfiguration
        if (ClientConfiguration.class.isAssignableFrom(configuration.getClass())) {
            this.configuration = (ClientConfiguration) configuration;
            if (this.endpoint != null) {
                try {
                    this.endpoint.setTimeout(this.configuration.getEndpointTimeout());
                } catch (CommunicationException ignored) {}
            }
        }
    }

    /**
     * Returns this {@link ClientConfiguration} that is currently active. This method will be useful
     * when implementing a new {@link com.damnvulnerableapp.networking.communication.server.Server}.
     *
     * @return This configuration that is currently active.
     * @see ClientConfiguration
     * @see Configurable
     * */
    @NonNull
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    // Factory method pattern. This outsources "raw" communication impl.
    /**
     * Constructs an instance of the low - level implementation for communication. E.g. this can be
     * a socket - based or file - mapping - based approach. Any {@link Client} - implementation has
     * to specify how communication will be done.
     *
     * @return Instance of a low - level implementation for communication.
     * @throws CommunicationException If establishing a connection fails. This should only happen, if
     *                                the {@link EndPoint} is used in conjunction with a
     *                                {@link com.damnvulnerableapp.networking.communication.server.Server}.
     * */
    protected abstract EndPoint createEndPoint() throws CommunicationException;
}
