package io.livekit.sdk.signaling;

import livekit.LivekitRtc;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * WebSocket client for LiveKit signaling protocol.
 * Handles connection, reconnection, and message parsing.
 */
public class SignalClient {
    private static final long PING_INTERVAL_MS = 10000;
    private static final long PING_TIMEOUT_MS = 15000;
    
    private final List<SignalListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private WebSocketClient wsClient;
    private SignalState state = SignalState.DISCONNECTED;
    private String url;
    private String token;
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> pingTimeoutTask;
    private long lastPongTimestamp;
    private int reconnectAttempts = 0;
    private int maxReconnectAttempts = 5;
    private long reconnectDelayMs = 1000;
    private boolean isReconnecting = false;

    public void connect(String url, String token) {
        if (state != SignalState.DISCONNECTED && state != SignalState.FAILED) {
            throw new IllegalStateException("Already connected or connecting");
        }
        
        this.url = url;
        this.token = token;
        this.isReconnecting = false;
        
        doConnect();
    }

    public void reconnect() {
        if (state == SignalState.RECONNECTING) {
            return;
        }
        
        this.isReconnecting = true;
        setState(SignalState.RECONNECTING);
        doConnect();
    }

    private void doConnect() {
        if (!isReconnecting) {
            setState(SignalState.CONNECTING);
        }
        
        try {
            String wsUrl = buildWebSocketUrl(url, token, isReconnecting);
            wsClient = createWebSocketClient(new URI(wsUrl));
            wsClient.connect();
        } catch (Exception e) {
            notifyError(e);
            handleConnectionFailure();
        }
    }

    private String buildWebSocketUrl(String baseUrl, String token, boolean reconnect) {
        // Convert http(s) to ws(s)
        String wsUrl = baseUrl.replace("https://", "wss://").replace("http://", "ws://");
        
        // Ensure it ends with /rtc
        if (!wsUrl.endsWith("/rtc")) {
            if (!wsUrl.endsWith("/")) {
                wsUrl += "/";
            }
            wsUrl += "rtc";
        }
        
        // Add query parameters
        wsUrl += "?access_token=" + token;
        wsUrl += "&protocol=13";  // Protocol version
        wsUrl += "&auto_subscribe=true";
        
        if (reconnect) {
            wsUrl += "&reconnect=1";
        }
        
        return wsUrl;
    }

    private WebSocketClient createWebSocketClient(URI uri) {
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                reconnectAttempts = 0;
                startPingPong();
                // State will be updated when JoinResponse is received
            }

            @Override
            public void onMessage(String message) {
                // Text messages not used in LiveKit protocol
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                handleBinaryMessage(bytes);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                stopPingPong();
                
                if (state == SignalState.CONNECTED || state == SignalState.RECONNECTING) {
                    handleConnectionFailure();
                } else {
                    setState(SignalState.DISCONNECTED);
                }
            }

            @Override
            public void onError(Exception ex) {
                notifyError(ex);
            }
        };
    }

    private void handleBinaryMessage(ByteBuffer bytes) {
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            
            LivekitRtc.SignalResponse response = LivekitRtc.SignalResponse.parseFrom(data);
            processSignalResponse(response);
        } catch (Exception e) {
            notifyError(e);
        }
    }

    private void processSignalResponse(LivekitRtc.SignalResponse response) {
        switch (response.getMessageCase()) {
            case JOIN:
                if (isReconnecting) {
                    setState(SignalState.CONNECTED);
                } else {
                    setState(SignalState.CONNECTED);
                }
                notifyJoinResponse(response.getJoin());
                break;
            case ANSWER:
                notifyAnswer(response.getAnswer());
                break;
            case OFFER:
                notifyOffer(response.getOffer());
                break;
            case TRICKLE:
                notifyTrickle(response.getTrickle());
                break;
            case UPDATE:
                notifyParticipantUpdate(response.getUpdate());
                break;
            case TRACK_PUBLISHED:
                notifyTrackPublished(response.getTrackPublished());
                break;
            case TRACK_UNPUBLISHED:
                notifyTrackUnpublished(response.getTrackUnpublished());
                break;
            case LEAVE:
                notifyLeave(response.getLeave());
                disconnect();
                break;
            case MUTE:
                notifyMuteTrack(response.getMute());
                break;
            case SPEAKERS_CHANGED:
                notifySpeakersChanged(response.getSpeakersChanged());
                break;
            case ROOM_UPDATE:
                notifyRoomUpdate(response.getRoomUpdate());
                break;
            case CONNECTION_QUALITY:
                notifyConnectionQuality(response.getConnectionQuality());
                break;
            case STREAM_STATE_UPDATE:
                notifyStreamStateUpdate(response.getStreamStateUpdate());
                break;
            case REFRESH_TOKEN:
                token = response.getRefreshToken();
                notifyRefreshToken(response.getRefreshToken());
                break;
            case RECONNECT:
                notifyReconnectResponse(response.getReconnect());
                break;
            case PONG:
                lastPongTimestamp = System.currentTimeMillis();
                notifyPong(response.getPong());
                break;
            case PONG_RESP:
                lastPongTimestamp = System.currentTimeMillis();
                notifyPong(response.getPongResp().getTimestamp());
                break;
            default:
                // Unknown message type
                break;
        }
    }

    public void disconnect() {
        stopPingPong();
        
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
        
        setState(SignalState.DISCONNECTED);
    }

    private void handleConnectionFailure() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            setState(SignalState.RECONNECTING);
            
            long delay = reconnectDelayMs * (1L << Math.min(reconnectAttempts - 1, 5));
            scheduler.schedule(this::reconnect, delay, TimeUnit.MILLISECONDS);
        } else {
            setState(SignalState.FAILED);
        }
    }

    private void startPingPong() {
        lastPongTimestamp = System.currentTimeMillis();
        
        pingTask = scheduler.scheduleAtFixedRate(() -> {
            sendPing();
            schedulePingTimeout();
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopPingPong() {
        if (pingTask != null) {
            pingTask.cancel(false);
            pingTask = null;
        }
        if (pingTimeoutTask != null) {
            pingTimeoutTask.cancel(false);
            pingTimeoutTask = null;
        }
    }

    private void sendPing() {
        LivekitRtc.Ping ping = LivekitRtc.Ping.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setPingReq(ping)
                .build();
        
        sendRequest(request);
    }

    private void schedulePingTimeout() {
        if (pingTimeoutTask != null) {
            pingTimeoutTask.cancel(false);
        }
        
        pingTimeoutTask = scheduler.schedule(() -> {
            if (System.currentTimeMillis() - lastPongTimestamp > PING_TIMEOUT_MS) {
                // Ping timeout, trigger reconnect
                handleConnectionFailure();
            }
        }, PING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    // Send methods for various signal requests
    public void sendRequest(LivekitRtc.SignalRequest request) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(request.toByteArray());
        }
    }

    public void sendOffer(LivekitRtc.SessionDescription offer) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setOffer(offer)
                .build();
        sendRequest(request);
    }

    public void sendAnswer(LivekitRtc.SessionDescription answer) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setAnswer(answer)
                .build();
        sendRequest(request);
    }

    public void sendTrickle(LivekitRtc.TrickleRequest trickle) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setTrickle(trickle)
                .build();
        sendRequest(request);
    }

    public void sendAddTrack(LivekitRtc.AddTrackRequest addTrack) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setAddTrack(addTrack)
                .build();
        sendRequest(request);
    }

    public void sendMuteTrack(LivekitRtc.MuteTrackRequest mute) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setMute(mute)
                .build();
        sendRequest(request);
    }

    public void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setSubscription(subscription)
                .build();
        sendRequest(request);
    }

    public void sendLeave() {
        LivekitRtc.LeaveRequest leave = LivekitRtc.LeaveRequest.newBuilder()
                .setCanReconnect(false)
                .build();
        
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setLeave(leave)
                .build();
        sendRequest(request);
    }

    public void sendSyncState(LivekitRtc.SyncState syncState) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setSyncState(syncState)
                .build();
        sendRequest(request);
    }

    public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {
        LivekitRtc.SignalRequest request = LivekitRtc.SignalRequest.newBuilder()
                .setUpdateMetadata(metadata)
                .build();
        sendRequest(request);
    }

    // State management
    public SignalState getState() {
        return state;
    }

    private void setState(SignalState newState) {
        if (this.state != newState) {
            this.state = newState;
            notifyStateChanged(newState);
        }
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public void setReconnectDelayMs(long reconnectDelayMs) {
        this.reconnectDelayMs = reconnectDelayMs;
    }

    // Listener management
    public void addListener(SignalListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SignalListener listener) {
        listeners.remove(listener);
    }

    // Notification methods
    private void notifyStateChanged(SignalState state) {
        for (SignalListener listener : listeners) {
            listener.onStateChanged(state);
        }
    }

    private void notifyJoinResponse(LivekitRtc.JoinResponse response) {
        for (SignalListener listener : listeners) {
            listener.onJoinResponse(response);
        }
    }

    private void notifyAnswer(LivekitRtc.SessionDescription answer) {
        for (SignalListener listener : listeners) {
            listener.onAnswer(answer);
        }
    }

    private void notifyOffer(LivekitRtc.SessionDescription offer) {
        for (SignalListener listener : listeners) {
            listener.onOffer(offer);
        }
    }

    private void notifyTrickle(LivekitRtc.TrickleRequest trickle) {
        for (SignalListener listener : listeners) {
            listener.onTrickle(trickle);
        }
    }

    private void notifyParticipantUpdate(LivekitRtc.ParticipantUpdate update) {
        for (SignalListener listener : listeners) {
            listener.onParticipantUpdate(update);
        }
    }

    private void notifyTrackPublished(LivekitRtc.TrackPublishedResponse response) {
        for (SignalListener listener : listeners) {
            listener.onTrackPublished(response);
        }
    }

    private void notifyTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response) {
        for (SignalListener listener : listeners) {
            listener.onTrackUnpublished(response);
        }
    }

    private void notifyLeave(LivekitRtc.LeaveRequest leave) {
        for (SignalListener listener : listeners) {
            listener.onLeave(leave);
        }
    }

    private void notifyMuteTrack(LivekitRtc.MuteTrackRequest mute) {
        for (SignalListener listener : listeners) {
            listener.onMuteTrack(mute);
        }
    }

    private void notifySpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged) {
        for (SignalListener listener : listeners) {
            listener.onSpeakersChanged(speakersChanged);
        }
    }

    private void notifyRoomUpdate(LivekitRtc.RoomUpdate roomUpdate) {
        for (SignalListener listener : listeners) {
            listener.onRoomUpdate(roomUpdate);
        }
    }

    private void notifyConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality) {
        for (SignalListener listener : listeners) {
            listener.onConnectionQuality(quality);
        }
    }

    private void notifyStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState) {
        for (SignalListener listener : listeners) {
            listener.onStreamStateUpdate(streamState);
        }
    }

    private void notifyRefreshToken(String token) {
        for (SignalListener listener : listeners) {
            listener.onRefreshToken(token);
        }
    }

    private void notifyReconnectResponse(LivekitRtc.ReconnectResponse response) {
        for (SignalListener listener : listeners) {
            listener.onReconnectResponse(response);
        }
    }

    private void notifyPong(long timestamp) {
        for (SignalListener listener : listeners) {
            listener.onPong(timestamp);
        }
    }

    private void notifyError(Exception e) {
        for (SignalListener listener : listeners) {
            listener.onError(e);
        }
    }

    public void shutdown() {
        disconnect();
        scheduler.shutdown();
    }
}
