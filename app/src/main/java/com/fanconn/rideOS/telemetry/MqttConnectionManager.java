package com.fanconn.rideOS.telemetry;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.fanconn.rideOS.menu.RiderMqttSyncWorker;
import com.fanconn.rideOS.telemetry.remotecommand.MqttCommandService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.SocketFactory;

public class MqttConnectionManager {

    private static final String TAG =
            "MqttConnectionManager";

    // ============================================================
    // MQTT
    // ============================================================

    private static final long RECONNECT_DELAY_SECONDS =
            5;

    private static final String BROKER_HOST =
            "103.174.148.249";

    private static final int BROKER_PORT =
            1883;

    // ============================================================
    // SINGLETON
    // ============================================================

    private static MqttConnectionManager instance;

    private final Context context;

    // ============================================================
    // CONNECTION STATE
    // ============================================================

    private final Object connectionLock =
            new Object();

    /*
     * Every new MQTT connection attempt receives
     * a new generation number.
     *
     * This prevents an old connection attempt from
     * modifying the state of a newer connection.
     */
    private long connectionGeneration =
            0L;

    private volatile MqttClient mqttClient;

    /*
     * Network used by the current MQTT client.
     */
    private volatile Network mqttNetwork;

    /*
     * Current validated Android network.
     */
    private volatile Network activeNetwork;

    private volatile boolean connected =
            false;

    private volatile boolean connecting =
            false;

    private volatile boolean networkAvailable =
            false;

    private volatile boolean shutdownRequested =
            false;

    // ============================================================
    // EXECUTOR
    // ============================================================

    /*
     * MQTT operations never run on the Android main thread.
     *
     * Multiple threads are required because an old MQTT
     * connect() may still be blocking while Android has already
     * switched to another network.
     */
    private final ScheduledExecutorService mqttExecutor =
            Executors.newScheduledThreadPool(4);

    private ScheduledFuture<?> reconnectFuture;

    // ============================================================
    // METADATA
    // ============================================================

    private final Map<String, Consumer<String>>
            metadataCallbacks =
            new ConcurrentHashMap<>();

    // ============================================================
    // COMMAND SERVICE
    // ============================================================

    private volatile MqttCommandService commandService;

    // ============================================================
    // NETWORK
    // ============================================================

    private ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback
            networkCallback;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    private MqttConnectionManager(
            Context context) {

        this.context =
                context.getApplicationContext();
    }

    public static synchronized MqttConnectionManager
    getInstance(
            Context context) {

        if (instance == null) {

            instance =
                    new MqttConnectionManager(
                            context
                    );
        }

        return instance;
    }

    // ============================================================
    // CONNECT
    // ============================================================

    public void connect() {

        shutdownRequested = false;

        if (!isNetworkAvailable()) {

            Log.d(
                    TAG,
                    "MQTT connect postponed. "
                            + "No validated network."
            );

            return;
        }

        synchronized (connectionLock) {

            /*
             * Already connected using the current network.
             */
            if (connected
                    && mqttClient != null
                    && mqttClient.isConnected()
                    && Objects.equals(
                    mqttNetwork,
                    activeNetwork
            )) {

                Log.d(
                        TAG,
                        "MQTT already connected on current "
                                + "network: "
                                + activeNetwork
                );

                return;
            }

            /*
             * A connection attempt is already running.
             */
            if (connecting) {

                Log.d(
                        TAG,
                        "MQTT connection already in progress"
                );

                return;
            }

            connecting = true;

            /*
             * New connection generation.
             */
            connectionGeneration++;

            Log.d(
                    TAG,
                    "Starting new MQTT connection generation = "
                            + connectionGeneration
            );
        }

        cancelReconnect();

        mqttExecutor.execute(
                this::performConnect
        );
    }

    // ============================================================
    // PERFORM CONNECT
    // ============================================================

    private void performConnect() {

        final long myGeneration;

        synchronized (connectionLock) {

            myGeneration =
                    connectionGeneration;
        }

        if (shutdownRequested) {

            clearConnectingIfCurrent(
                    myGeneration
            );

            return;
        }

        // --------------------------------------------------------
        // SELECT NETWORK
        // --------------------------------------------------------

        Network selectedNetwork =
                activeNetwork;

        if (selectedNetwork == null) {

            selectedNetwork =
                    getValidatedActiveNetwork();
        }

        if (selectedNetwork == null
                || !isNetworkValidated(
                selectedNetwork
        )) {

            Log.w(
                    TAG,
                    "Cannot connect. "
                            + "No validated network."
            );

            clearConnectingIfCurrent(
                    myGeneration
            );

            scheduleReconnect();

            return;
        }

        /*
         * Make sure this connection generation still owns
         * the connection attempt.
         */
        if (!isGenerationCurrent(
                myGeneration
        )) {

            Log.d(
                    TAG,
                    "Ignoring obsolete MQTT connection "
                            + "generation: "
                            + myGeneration
            );

            return;
        }

        /*
         * Make sure the selected network is still the
         * currently active network.
         */
        if (!Objects.equals(
                selectedNetwork,
                activeNetwork
        )) {

            Log.w(
                    TAG,
                    "Network changed before MQTT connection. "
                            + "Retrying."
            );

            clearConnectingIfCurrent(
                    myGeneration
            );

            scheduleReconnect();

            return;
        }

        final Network network =
                selectedNetwork;

        final String clientId =
                "RideOS-"
                        + System.currentTimeMillis();

        MqttClient client =
                null;

        try {

            Log.d(
                    TAG,
                    "Creating MQTT client: "
                            + clientId
                            + " | network="
                            + network
                            + " | generation="
                            + myGeneration
            );

            client =
                    new MqttClient(
                            MqttConfig.BROKER_URL,
                            clientId,
                            null
                    );

            // ----------------------------------------------------
            // MQTT OPTIONS
            // ----------------------------------------------------

            MqttConnectOptions options =
                    new MqttConnectOptions();

            /*
             * IMPORTANT:
             *
             * Bind the MQTT socket to the selected Android
             * network.
             *
             * Your latest test proved that the IoT SIM can
             * successfully reach:
             *
             * 103.174.148.249:1883
             *
             * using this network.
             */
            options.setSocketFactory(
                    network.getSocketFactory()
            );

            options.setUserName(
                    MqttConfig.MQTT_USERNAME
            );

            options.setPassword(
                    MqttConfig.MQTT_PASSWORD
                            .toCharArray()
            );

            /*
             * We manage reconnect ourselves because we need
             * explicit network switching.
             */
            options.setAutomaticReconnect(
                    false
            );

            options.setCleanSession(
                    true
            );

            options.setConnectionTimeout(
                    10
            );

            options.setKeepAliveInterval(
                    20
            );

            configureLastWill(
                    options
            );

            // ----------------------------------------------------
            // CALLBACK
            // ----------------------------------------------------

            setupCallback(
                    client,
                    network,
                    myGeneration
            );

            // ----------------------------------------------------
            // REGISTER CLIENT
            // ----------------------------------------------------

            synchronized (connectionLock) {

                /*
                 * Do not register this client if another
                 * connection generation has already replaced it.
                 */
                if (connectionGeneration
                        != myGeneration) {

                    Log.w(
                            TAG,
                            "Connection generation changed "
                                    + "before client registration. "
                                    + "Ignoring generation="
                                    + myGeneration
                    );

                    closeClientQuietly(
                            client
                    );

                    return;
                }

                mqttClient =
                        client;

                mqttNetwork =
                        network;

                connected =
                        false;
            }

            // ----------------------------------------------------
            // FINAL CHECK
            // ----------------------------------------------------

            if (shutdownRequested
                    || !isGenerationCurrent(
                    myGeneration
            )
                    || !Objects.equals(
                    activeNetwork,
                    network
            )
                    || !isNetworkValidated(
                    network
            )) {

                Log.w(
                        TAG,
                        "Network/client changed before MQTT "
                                + "connect. Aborting generation="
                                + myGeneration
                );

                clearClientIfCurrent(
                        client,
                        myGeneration
                );

                closeClientQuietly(
                        client
                );

                scheduleReconnect();

                return;
            }

            // ----------------------------------------------------
            // CONNECT
            // ----------------------------------------------------

            Log.d(
                    TAG,
                    "Starting MQTT connection -> "
                            + MqttConfig.BROKER_URL
                            + " | network="
                            + network
                            + " | generation="
                            + myGeneration
            );

            try {

                /*
                 * Blocking operation.
                 *
                 * mqttExecutor has 4 threads, so this does not
                 * block Android network callbacks.
                 */
                client.connect(
                        options
                );

                // ------------------------------------------------
                // CONNECTION RESULT
                // ------------------------------------------------

                if (!client.isConnected()) {

                    throw new MqttException(
                            MqttException
                                    .REASON_CODE_CLIENT_NOT_CONNECTED
                    );
                }

                synchronized (connectionLock) {

                    /*
                     * Only the current client/generation is allowed
                     * to become the active MQTT connection.
                     */
                    if (mqttClient == client
                            && connectionGeneration
                            == myGeneration
                            && Objects.equals(
                            mqttNetwork,
                            network
                    )
                            && Objects.equals(
                            activeNetwork,
                            network
                    )
                            && isNetworkValidated(
                            network
                    )) {

                        connected =
                                true;

                        connecting =
                                false;

                        Log.d(
                                TAG,
                                "MQTT CONNECT SUCCESS | "
                                        + "network="
                                        + network
                                        + " | generation="
                                        + myGeneration
                        );

                    } else {

                        Log.w(
                                TAG,
                                "MQTT connected but client is stale. "
                                        + "Ignoring generation="
                                        + myGeneration
                        );

                        closeClientQuietly(
                                client
                        );

                        return;
                    }
                }

            } catch (Exception e) {

                /*
                 * CRITICAL:
                 *
                 * Only this generation may modify the
                 * connecting/client state.
                 */
                synchronized (connectionLock) {

                    if (mqttClient == client
                            && connectionGeneration
                            == myGeneration) {

                        mqttClient =
                                null;

                        mqttNetwork =
                                null;

                        connected =
                                false;

                        connecting =
                                false;
                    }
                }

                closeClientQuietly(
                        client
                );

                Log.e(
                        TAG,
                        "MQTT CONNECT FAILED | "
                                + "network="
                                + network
                                + " | generation="
                                + myGeneration,
                        e
                );

                scheduleReconnect();
            }

        } catch (Exception e) {

            synchronized (connectionLock) {

                if (connectionGeneration
                        == myGeneration) {

                    connected =
                            false;

                    connecting =
                            false;

                    if (mqttClient == client) {

                        mqttClient =
                                null;

                        mqttNetwork =
                                null;
                    }
                }
            }

            closeClientQuietly(
                    client
            );

            Log.e(
                    TAG,
                    "MQTT connection setup failed | "
                            + "generation="
                            + myGeneration,
                    e
            );

            scheduleReconnect();
        }
    }

    // ============================================================
    // MQTT CALLBACK
    // ============================================================

    private void setupCallback(
            MqttClient client,
            Network clientNetwork,
            long clientGeneration) {

        client.setCallback(
                new MqttCallbackExtended() {

                    @Override
                    public void connectComplete(
                            boolean reconnect,
                            String serverURI) {

                        boolean stale =
                                false;

                        synchronized (connectionLock) {

                            /*
                             * Ignore callbacks from old clients.
                             */
                            if (mqttClient != client) {

                                Log.w(
                                        TAG,
                                        "Ignoring callback from "
                                                + "stale MQTT client."
                                );

                                return;
                            }

                            /*
                             * Ignore callbacks from old
                             * connection generations.
                             */
                            if (connectionGeneration
                                    != clientGeneration) {

                                Log.w(
                                        TAG,
                                        "Ignoring callback from "
                                                + "old MQTT generation="
                                                + clientGeneration
                                );

                                return;
                            }

                            /*
                             * Verify network.
                             */
                            if (!Objects.equals(
                                    activeNetwork,
                                    clientNetwork
                            )
                                    || !isNetworkValidated(
                                    clientNetwork
                            )) {

                                Log.w(
                                        TAG,
                                        "MQTT connected on stale "
                                                + "network. Closing client."
                                );

                                mqttClient =
                                        null;

                                mqttNetwork =
                                        null;

                                connected =
                                        false;

                                connecting =
                                        false;

                                stale =
                                        true;

                            } else {

                                connected =
                                        true;

                                connecting =
                                        false;
                            }
                        }

                        if (stale) {

                            closeClientQuietly(
                                    client
                            );

                            scheduleReconnect();

                            return;
                        }

                        cancelReconnect();

                        Log.d(
                                TAG,
                                "MQTT CONNECTED"
                                        + " | reconnect="
                                        + reconnect
                                        + " | server="
                                        + serverURI
                                        + " | network="
                                        + clientNetwork
                                        + " | generation="
                                        + clientGeneration
                        );

                        publishOnlineStatus();

                        subscribeCommandTopic();

                        subscribeMetadataResponseTopic();

                        uploadStagedTelemetry();
                        RiderMqttSyncWorker.sync(context);
                    }

                    @Override
                    public void connectionLost(
                            Throwable cause) {

                        synchronized (connectionLock) {

                            /*
                             * Ignore connectionLost from an
                             * old client.
                             */
                            if (mqttClient != client) {

                                return;
                            }

                            if (connectionGeneration
                                    != clientGeneration) {

                                return;
                            }

                            connected =
                                    false;

                            connecting =
                                    false;
                        }

                        Log.e(
                                TAG,
                                "MQTT CONNECTION LOST"
                                        + " | network="
                                        + clientNetwork
                                        + " | generation="
                                        + clientGeneration,
                                cause
                        );

                        scheduleReconnect();
                    }

                    @Override
                    public void messageArrived(
                            String topic,
                            MqttMessage message) {

                        try {

                            if (topic == null
                                    || message == null) {

                                return;
                            }

                            Log.d(
                                    TAG,
                                    "MQTT message received -> "
                                            + topic
                            );

                            if (topic.startsWith(
                                    MqttConfig
                                            .TELEMETRY_TOPIC_PREFIX
                            )
                                    && topic.endsWith(
                                    "/metadata/response"
                            )) {

                                handleMetadataResponse(
                                        topic,
                                        message
                                );
                            }

                        } catch (Exception e) {

                            Log.e(
                                    TAG,
                                    "Error handling MQTT message",
                                    e
                            );
                        }
                    }

                    @Override
                    public void deliveryComplete(
                            IMqttDeliveryToken token) {

                        // QoS delivery callback.
                    }
                }
        );
    }

    // ============================================================
    // NETWORK MONITORING
    // ============================================================

    public synchronized void startNetworkMonitoring() {

        if (networkCallback != null) {

            Log.d(
                    TAG,
                    "Network monitoring already started"
            );

            return;
        }

        connectivityManager =
                (ConnectivityManager)
                        context.getSystemService(
                                Context.CONNECTIVITY_SERVICE
                        );

        if (connectivityManager == null) {

            Log.e(
                    TAG,
                    "ConnectivityManager unavailable"
            );

            return;
        }

        networkCallback =
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(
                            Network network) {

                        Log.d(
                                TAG,
                                "NETWORK AVAILABLE: "
                                        + network
                        );

                        if (!isNetworkValidated(
                                network
                        )) {

                            Log.d(
                                    TAG,
                                    "Network available but not "
                                            + "validated yet: "
                                            + network
                            );

                            return;
                        }

                        handleNetworkChanged(
                                network
                        );
                    }

                    @Override
                    public void onCapabilitiesChanged(
                            Network network,
                            NetworkCapabilities capabilities) {

                        boolean validated =
                                capabilities != null
                                        && capabilities
                                        .hasCapability(
                                                NetworkCapabilities
                                                        .NET_CAPABILITY_INTERNET
                                        )
                                        && capabilities
                                        .hasCapability(
                                                NetworkCapabilities
                                                        .NET_CAPABILITY_VALIDATED
                                        );

                        Log.d(
                                TAG,
                                "NETWORK CAPABILITIES CHANGED -> "
                                        + network
                                        + " | validated="
                                        + validated
                        );

                        if (validated) {

                            /*
                             * This method now safely ignores
                             * repeated callbacks for the same
                             * Network.
                             */
                            handleNetworkChanged(
                                    network
                            );

                        } else {

                            synchronized (
                                    connectionLock
                            ) {

                                if (Objects.equals(
                                        activeNetwork,
                                        network
                                )) {

                                    Log.w(
                                            TAG,
                                            "Active network lost "
                                                    + "validation."
                                    );

                                    activeNetwork =
                                            null;

                                    networkAvailable =
                                            false;
                                }
                            }

                            forceDisconnectForNetworkChange(
                                    network
                            );

                            scheduleReconnect();
                        }
                    }

                    @Override
                    public void onLost(
                            Network network) {

                        Log.w(
                                TAG,
                                "NETWORK LOST: "
                                        + network
                        );

                        boolean wasActive;

                        synchronized (
                                connectionLock
                        ) {

                            wasActive =
                                    Objects.equals(
                                            activeNetwork,
                                            network
                                    );

                            if (wasActive) {

                                activeNetwork =
                                        null;

                                networkAvailable =
                                        false;
                            }
                        }

                        if (wasActive) {

                            forceDisconnectForNetworkChange(
                                    network
                            );

                            scheduleReconnect();

                        } else {

                            Log.d(
                                    TAG,
                                    "Lost network was not active "
                                            + "MQTT network."
                            );
                        }
                    }
                };

        try {

            connectivityManager
                    .registerDefaultNetworkCallback(
                            networkCallback
                    );

            updateNetworkState();

            Log.d(
                    TAG,
                    "Network monitoring started"
                            + " | network="
                            + activeNetwork
                            + " | available="
                            + networkAvailable
            );

            if (networkAvailable) {

                connect();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to register network callback",
                    e
            );
        }
    }

    // ============================================================
    // NETWORK CHANGE
    // ============================================================

    private void handleNetworkChanged(
            Network newNetwork) {

        if (newNetwork == null) {

            return;
        }

        Network previousNetwork =
                activeNetwork;

        boolean sameNetwork =
                Objects.equals(
                        previousNetwork,
                        newNetwork
                );

        activeNetwork =
                newNetwork;

        networkAvailable =
                true;

        logNetworkTransport(
                newNetwork
        );

        Log.d(
                TAG,
                "VALIDATED NETWORK ACTIVE -> "
                        + newNetwork
                        + " | previous="
                        + previousNetwork
                        + " | sameNetwork="
                        + sameNetwork
        );

        // ========================================================
        // SAME NETWORK
        // ========================================================

        if (sameNetwork) {

            /*
             * IMPORTANT:
             *
             * Repeated onCapabilitiesChanged() callbacks
             * must NOT destroy the MQTT connection.
             */
            if (!isConnected()) {

                Log.d(
                        TAG,
                        "Same network is active but MQTT "
                                + "is disconnected. Starting "
                                + "MQTT connection."
                );

                connect();

            } else {

                Log.d(
                        TAG,
                        "Same network and MQTT already "
                                + "connected. No reconnect "
                                + "required."
                );
            }

            return;
        }

        // ========================================================
        // ACTUAL NETWORK CHANGE
        // ========================================================

        Log.w(
                TAG,
                "NETWORK ACTUALLY CHANGED -> "
                        + previousNetwork
                        + " -> "
                        + newNetwork
        );

        /*
         * Only test the broker when the actual active
         * network changes.
         */
        testBrokerTcpConnection(
                newNetwork
        );

        /*
         * Mark old MQTT client stale.
         *
         * IMPORTANT:
         * This also creates a new connection generation,
         * preventing callbacks from the old connection from
         * affecting the new connection.
         */
        if (previousNetwork != null) {

            forceDisconnectForNetworkChange(
                    previousNetwork
            );
        }

        /*
         * Connect using the new network.
         */
        connect();
    }

    // ============================================================
    // FORCE DISCONNECT OLD NETWORK
    // ============================================================

    private void forceDisconnectForNetworkChange(
            Network oldNetwork) {

        MqttClient oldClient =
                null;

        synchronized (connectionLock) {

            if (mqttClient != null
                    && Objects.equals(
                    mqttNetwork,
                    oldNetwork
            )) {

                Log.w(
                        TAG,
                        "Marking old MQTT client stale "
                                + "for network: "
                                + oldNetwork
                );

                /*
                 * Invalidate old connection generation.
                 */
                connectionGeneration++;

                oldClient =
                        mqttClient;

                mqttClient =
                        null;

                mqttNetwork =
                        null;

                connected =
                        false;

                connecting =
                        false;
            }
        }

        /*
         * Do not close while holding connectionLock.
         */
        if (oldClient != null) {

            closeClientQuietly(
                    oldClient
            );
        }
    }

    // ============================================================
    // NETWORK STATE
    // ============================================================

    private void updateNetworkState() {

        if (connectivityManager == null) {

            networkAvailable =
                    false;

            activeNetwork =
                    null;

            return;
        }

        Network network =
                connectivityManager
                        .getActiveNetwork();

        if (network == null) {

            networkAvailable =
                    false;

            activeNetwork =
                    null;

            return;
        }

        if (isNetworkValidated(
                network
        )) {

            activeNetwork =
                    network;

            networkAvailable =
                    true;

        } else {

            networkAvailable =
                    false;

            if (Objects.equals(
                    activeNetwork,
                    network
            )) {

                activeNetwork =
                        null;
            }
        }

        Log.d(
                TAG,
                "Internet state -> "
                        + networkAvailable
                        + " | network="
                        + activeNetwork
        );
    }

    // ============================================================
    // GET VALIDATED ACTIVE NETWORK
    // ============================================================

    private Network getValidatedActiveNetwork() {

        if (connectivityManager == null) {

            return null;
        }

        Network network =
                connectivityManager
                        .getActiveNetwork();

        if (network != null
                && isNetworkValidated(
                network
        )) {

            return network;
        }

        return null;
    }

    // ============================================================
    // IS NETWORK VALIDATED
    // ============================================================

    private boolean isNetworkValidated(
            Network network) {

        if (network == null
                || connectivityManager == null) {

            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager
                        .getNetworkCapabilities(
                                network
                        );

        return capabilities != null
                && capabilities.hasCapability(
                NetworkCapabilities
                        .NET_CAPABILITY_INTERNET
        )
                && capabilities.hasCapability(
                NetworkCapabilities
                        .NET_CAPABILITY_VALIDATED
        );
    }

    // ============================================================
    // IS NETWORK AVAILABLE
    // ============================================================

    private boolean isNetworkAvailable() {

        Network network =
                getUsableActiveNetwork();

        if (network == null) {

            networkAvailable = false;

            return false;
        }

        activeNetwork = network;
        networkAvailable = true;

        return true;
    }

    private Network getUsableActiveNetwork() {

        if (connectivityManager == null) {
            return null;
        }

        Network network =
                connectivityManager.getActiveNetwork();

        if (network != null && isNetworkUsable(network)) {
            return network;
        }

        return null;
    }

    private boolean isNetworkUsable(Network network) {

        if (network == null
                || connectivityManager == null) {

            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(
                        network
                );

        return capabilities != null
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        );
    }
    // ============================================================
    // RECONNECT
    // ============================================================

    private void scheduleReconnect() {

        if (shutdownRequested) {

            return;
        }

        if (!isNetworkAvailable()) {

            Log.d(
                    TAG,
                    "Reconnect not scheduled. "
                            + "No validated network."
            );

            return;
        }

        synchronized (connectionLock) {

            if (connected
                    && mqttClient != null
                    && mqttClient.isConnected()) {

                return;
            }

            if (connecting) {

                Log.d(
                        TAG,
                        "Reconnect not scheduled. "
                                + "Connection already in progress."
                );

                return;
            }
        }

        synchronized (this) {

            if (reconnectFuture != null
                    && !reconnectFuture.isDone()
                    && !reconnectFuture.isCancelled()) {

                Log.d(
                        TAG,
                        "Reconnect already scheduled."
                );

                return;
            }

            Log.d(
                    TAG,
                    "Scheduling MQTT reconnect in "
                            + RECONNECT_DELAY_SECONDS
                            + " seconds"
            );

            reconnectFuture =
                    mqttExecutor.schedule(
                            () -> {

                                synchronized (
                                        MqttConnectionManager.this
                                ) {

                                    reconnectFuture =
                                            null;
                                }

                                if (shutdownRequested) {

                                    return;
                                }

                                if (!isNetworkAvailable()) {

                                    Log.d(
                                            TAG,
                                            "Reconnect cancelled. "
                                                    + "No validated network."
                                    );

                                    return;
                                }

                                if (isConnected()) {

                                    return;
                                }

                                Log.d(
                                        TAG,
                                        "Retrying MQTT connection"
                                );

                                connect();

                            },
                            RECONNECT_DELAY_SECONDS,
                            TimeUnit.SECONDS
                    );
        }
    }

    // ============================================================
    // CANCEL RECONNECT
    // ============================================================

    private synchronized void cancelReconnect() {

        if (reconnectFuture != null) {

            reconnectFuture.cancel(
                    false
            );

            reconnectFuture =
                    null;
        }
    }

    // ============================================================
    // GENERATION HELPERS
    // ============================================================

    private boolean isGenerationCurrent(
            long generation) {

        synchronized (connectionLock) {

            return connectionGeneration
                    == generation;
        }
    }

    private void clearConnectingIfCurrent(
            long generation) {

        synchronized (connectionLock) {

            if (connectionGeneration
                    == generation) {

                connecting =
                        false;
            }
        }
    }

    private void clearClientIfCurrent(
            MqttClient client,
            long generation) {

        synchronized (connectionLock) {

            if (connectionGeneration
                    == generation
                    && mqttClient == client) {

                mqttClient =
                        null;

                mqttNetwork =
                        null;

                connected =
                        false;

                connecting =
                        false;
            }
        }
    }

    // ============================================================
    // CONNECTION STATE
    // ============================================================

    public boolean isConnected() {

        MqttClient client =
                mqttClient;

        return connected
                && client != null
                && client.isConnected();
    }

    public MqttClient getMqttClient() {

        return mqttClient;
    }

    // ============================================================
    // COMMAND SERVICE
    // ============================================================

    public void setCommandService(
            MqttCommandService commandService) {

        this.commandService =
                commandService;
    }

    private void subscribeCommandTopic() {

        MqttCommandService service =
                commandService;

        if (service == null) {

            Log.w(
                    TAG,
                    "Command service is null. "
                            + "Skipping command subscription."
            );

            return;
        }

        String vin =
                getVinFromTelemetryManager();

        if (vin == null
                || vin.trim().isEmpty()) {

            Log.w(
                    TAG,
                    "VIN unavailable. "
                            + "Skipping command subscription."
            );

            return;
        }

        try {

            service.subscribe(
                    vin
            );

            Log.d(
                    TAG,
                    "Command subscription requested: "
                            + vin
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Command subscription failed",
                    e
            );
        }
    }

    // ============================================================
    // METADATA SUBSCRIPTION
    // ============================================================

    private void subscribeMetadataResponseTopic() {

        MqttClient client =
                mqttClient;

        if (!isConnected()
                || client == null) {

            return;
        }

        try {

            String topic =
                    MqttConfig
                            .TELEMETRY_TOPIC_PREFIX
                            + "+"
                            + "/metadata/response";

            client.subscribe(
                    topic,
                    1
            );

            Log.d(
                    TAG,
                    "Subscribed to metadata response topic: "
                            + topic
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Metadata response subscription failed",
                    e
            );
        }
    }

    // ============================================================
    // METADATA REQUEST
    // ============================================================

    public void requestMetadata(
            String vin,
            Consumer<String> callback) {

        if (vin == null
                || vin.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Cannot request metadata. VIN is empty."
            );

            return;
        }

        MqttClient client =
                mqttClient;

        if (!isConnected()
                || client == null) {

            Log.e(
                    TAG,
                    "Cannot request metadata. "
                            + "MQTT not connected."
            );

            return;
        }

        if (callback != null) {

            metadataCallbacks.put(
                    vin,
                    callback
            );
        }

        String topic =
                MqttConfig
                        .TELEMETRY_TOPIC_PREFIX
                        + vin
                        + "/metadata/request";

        String payload =
                "{\"request\":\"get_metadata\"}";

        try {

            MqttMessage message =
                    new MqttMessage(
                            payload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            message.setQos(
                    1
            );

            message.setRetained(
                    false
            );

            client.publish(
                    topic,
                    message
            );

            Log.d(
                    TAG,
                    "Metadata request published -> VIN: "
                            + vin
            );

        } catch (Exception e) {

            metadataCallbacks.remove(
                    vin
            );

            Log.e(
                    TAG,
                    "Failed to publish metadata request -> VIN: "
                            + vin,
                    e
            );
        }
    }

    // ============================================================
    // METADATA RESPONSE
    // ============================================================

    private void handleMetadataResponse(
            String topic,
            MqttMessage message) {

        try {

            String[] parts =
                    topic.split("/");

            if (parts.length != 4) {

                Log.e(
                        TAG,
                        "Invalid metadata response topic: "
                                + topic
                );

                return;
            }

            String vin =
                    parts[1];

            String payload =
                    new String(
                            message.getPayload(),
                            StandardCharsets.UTF_8
                    );

            Log.d(
                    TAG,
                    "Metadata response received -> VIN: "
                            + vin
            );

            Consumer<String> callback =
                    metadataCallbacks.remove(
                            vin
                    );

            if (callback != null) {

                try {

                    callback.accept(
                            payload
                    );

                } catch (Exception e) {

                    Log.e(
                            TAG,
                            "Metadata callback failed -> VIN: "
                                    + vin,
                            e
                    );
                }

            } else {

                Log.w(
                        TAG,
                        "No metadata callback waiting for VIN: "
                                + vin
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to handle metadata response",
                    e
            );
        }
    }

    // ============================================================
    // STAGED TELEMETRY
    // ============================================================

    // ============================================================
// STAGED TELEMETRY
// ============================================================

    private void uploadStagedTelemetry() {

        try {

            Constraints constraints =
                    new Constraints.Builder()
                            .setRequiredNetworkType(
                                    NetworkType.CONNECTED
                            )
                            .build();

            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(
                            TelemetryUploadWorker.class
                    )
                            .setConstraints(
                                    constraints
                            )
                            .setBackoffCriteria(
                                    androidx.work.BackoffPolicy.LINEAR,
                                    5,
                                    TimeUnit.SECONDS
                            )
                            .build();

            WorkManager workManager =
                    WorkManager.getInstance(
                            context
                    );

            workManager.enqueueUniqueWork(
                    "staged_telemetry_upload",
                    ExistingWorkPolicy.REPLACE,
                    request
            );

            Log.d(
                    TAG,
                    "STAGED UPLOAD WORK ENQUEUED"
                            + " | policy=REPLACE"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to start staged telemetry upload",
                    e
            );
        }
    }

    // ============================================================
    // LAST WILL
    // ============================================================

    private void configureLastWill(
            MqttConnectOptions options) {

        String vin =
                getVinFromTelemetryManager();

        if (vin == null
                || vin.trim().isEmpty()) {

            return;
        }

        String topic =
                MqttConfig
                        .TELEMETRY_TOPIC_PREFIX
                        + vin
                        + "/status";

        options.setWill(
                topic,
                "offline".getBytes(
                        StandardCharsets.UTF_8
                ),
                1,
                true
        );
    }

    // ============================================================
    // ONLINE STATUS
    // ============================================================

    private void publishOnlineStatus() {

        try {

            if (!isConnected()) {

                return;
            }

            String vin =
                    getVinFromTelemetryManager();

            if (vin == null
                    || vin.trim().isEmpty()) {

                return;
            }

            String topic =
                    MqttConfig
                            .TELEMETRY_TOPIC_PREFIX
                            + vin
                            + "/status";

            MqttMessage message =
                    new MqttMessage(
                            "online".getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            message.setQos(
                    1
            );

            message.setRetained(
                    true
            );

            MqttClient client =
                    mqttClient;

            if (client != null
                    && client.isConnected()) {

                client.publish(
                        topic,
                        message
                );

                Log.d(
                        TAG,
                        "Published status -> online"
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to publish online status",
                    e
            );
        }
    }

    // ============================================================
    // VIN
    // ============================================================

    private String getVinFromTelemetryManager() {

        try {

            return VehicleTelemetryManager
                    .getInstance()
                    .getVin();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to get VIN",
                    e
            );

            return null;
        }
    }

    // ============================================================
    // CLOSE CLIENT
    // ============================================================

    private void closeClientQuietly(
            MqttClient client) {

        if (client == null) {

            return;
        }

        try {

            if (client.isConnected()) {

                client.disconnect();
            }

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "MQTT disconnect during cleanup failed",
                    e
            );
        }

        try {

            client.close();

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "MQTT close during cleanup failed",
                    e
            );
        }
    }

    // ============================================================
    // DISCONNECT
    // ============================================================

    public void disconnect() {

        shutdownRequested =
                true;

        cancelReconnect();

        synchronized (connectionLock) {

            /*
             * Invalidate every running connection attempt.
             */
            connectionGeneration++;

            connected =
                    false;

            connecting =
                    false;
        }

        mqttExecutor.execute(
                () -> {

                    MqttClient client;

                    synchronized (connectionLock) {

                        client =
                                mqttClient;

                        mqttClient =
                                null;

                        mqttNetwork =
                                null;
                    }

                    closeClientQuietly(
                            client
                    );

                    Log.d(
                            TAG,
                            "MQTT disconnected."
                    );
                }
        );
    }

    // ============================================================
    // TCP BROKER TEST
    // ============================================================

    private void testBrokerTcpConnection(
            Network network) {

        if (network == null) {

            Log.e(
                    TAG,
                    "TCP TEST: network=null"
            );

            return;
        }

        mqttExecutor.execute(
                () -> {

                    Socket socket =
                            null;

                    try {

                        Log.d(
                                TAG,
                                "================================================"
                        );

                        Log.d(
                                TAG,
                                "TCP TEST START"
                        );

                        Log.d(
                                TAG,
                                "Network = "
                                        + network
                        );

                        Log.d(
                                TAG,
                                "Transport test -> "
                                        + BROKER_HOST
                                        + ":"
                                        + BROKER_PORT
                        );

                        /*
                         * Explicitly bind this diagnostic socket
                         * to the supplied Android Network.
                         */
                        SocketFactory socketFactory =
                                network.getSocketFactory();

                        socket =
                                socketFactory
                                        .createSocket();

                        long startTime =
                                System.currentTimeMillis();

                        socket.connect(
                                new InetSocketAddress(
                                        BROKER_HOST,
                                        BROKER_PORT
                                ),
                                5000
                        );

                        long elapsed =
                                System.currentTimeMillis()
                                        - startTime;

                        Log.d(
                                TAG,
                                "TCP TEST SUCCESS"
                        );

                        Log.d(
                                TAG,
                                "Network "
                                        + network
                                        + " CAN reach "
                                        + BROKER_HOST
                                        + ":"
                                        + BROKER_PORT
                        );

                        Log.d(
                                TAG,
                                "TCP connection time = "
                                        + elapsed
                                        + " ms"
                        );

                        Log.d(
                                TAG,
                                "Local socket = "
                                        + socket
                                        .getLocalSocketAddress()
                        );

                        Log.d(
                                TAG,
                                "Remote socket = "
                                        + socket
                                        .getRemoteSocketAddress()
                        );

                        Log.d(
                                TAG,
                                "================================================"
                        );

                    } catch (
                            SocketTimeoutException e
                    ) {

                        Log.e(
                                TAG,
                                "================================================"
                        );

                        Log.e(
                                TAG,
                                "TCP TEST FAILED - TIMEOUT"
                        );

                        Log.e(
                                TAG,
                                "Network "
                                        + network
                                        + " could NOT reach "
                                        + BROKER_HOST
                                        + ":"
                                        + BROKER_PORT
                                        + " within 5 seconds.",
                                e
                        );

                        Log.e(
                                TAG,
                                "================================================"
                        );

                    } catch (
                            ConnectException e
                    ) {

                        Log.e(
                                TAG,
                                "================================================"
                        );

                        Log.e(
                                TAG,
                                "TCP TEST FAILED - CONNECTION REFUSED/FAILED"
                        );

                        Log.e(
                                TAG,
                                "Network = "
                                        + network
                        );

                        Log.e(
                                TAG,
                                "Broker = "
                                        + BROKER_HOST
                                        + ":"
                                        + BROKER_PORT,
                                e
                        );

                        Log.e(
                                TAG,
                                "================================================"
                        );

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "================================================"
                        );

                        Log.e(
                                TAG,
                                "TCP TEST FAILED"
                        );

                        Log.e(
                                TAG,
                                "Network = "
                                        + network
                        );

                        Log.e(
                                TAG,
                                "Broker = "
                                        + BROKER_HOST
                                        + ":"
                                        + BROKER_PORT,
                                e
                        );

                        Log.e(
                                TAG,
                                "================================================"
                        );

                    } finally {

                        if (socket != null) {

                            try {

                                socket.close();

                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
        );
    }

    // ============================================================
    // NETWORK TRANSPORT LOG
    // ============================================================

    private void logNetworkTransport(
            Network network) {

        if (network == null) {

            Log.w(
                    TAG,
                    "NETWORK TRANSPORT: network=null"
            );

            return;
        }

        if (connectivityManager == null) {

            Log.w(
                    TAG,
                    "NETWORK TRANSPORT: "
                            + "ConnectivityManager=null"
            );

            return;
        }

        NetworkCapabilities capabilities =
                connectivityManager
                        .getNetworkCapabilities(
                                network
                        );

        if (capabilities == null) {

            Log.w(
                    TAG,
                    "NETWORK TRANSPORT: capabilities=null"
            );

            return;
        }

        boolean wifi =
                capabilities.hasTransport(
                        NetworkCapabilities
                                .TRANSPORT_WIFI
                );

        boolean cellular =
                capabilities.hasTransport(
                        NetworkCapabilities
                                .TRANSPORT_CELLULAR
                );

        boolean ethernet =
                capabilities.hasTransport(
                        NetworkCapabilities
                                .TRANSPORT_ETHERNET
                );

        boolean vpn =
                capabilities.hasTransport(
                        NetworkCapabilities
                                .TRANSPORT_VPN
                );

        boolean internet =
                capabilities.hasCapability(
                        NetworkCapabilities
                                .NET_CAPABILITY_INTERNET
                );

        boolean validated =
                capabilities.hasCapability(
                        NetworkCapabilities
                                .NET_CAPABILITY_VALIDATED
                );

        Log.d(
                TAG,
                "NETWORK TRANSPORT -> "
                        + network
                        + " | WIFI="
                        + wifi
                        + " | CELLULAR="
                        + cellular
                        + " | ETHERNET="
                        + ethernet
                        + " | VPN="
                        + vpn
                        + " | INTERNET="
                        + internet
                        + " | VALIDATED="
                        + validated
        );
    }
}