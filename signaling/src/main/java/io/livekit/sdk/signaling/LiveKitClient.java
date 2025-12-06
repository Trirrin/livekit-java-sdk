package io.livekit.sdk.signaling;

import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomSignalHandler;
import livekit.LivekitRtc;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main client class that coordinates Room with SignalClient.
 * This is the primary entry point for connecting to a LiveKit room.
 */
public class LiveKitClient implements SignalListener {
    private final Room room;
    private final SignalClient signalClient;
    private final RoomSignalHandler signalHandler;
    private CompletableFuture<Room> connectFuture;

    public LiveKitClient() {
        this(new RoomOptions());
    }

    public LiveKitClient(RoomOptions options) {
        this.room = new Room(options);
        this.signalClient = new SignalClient();
        this.signalHandler = room.getSignalHandler();
        this.signalClient.addListener(this);
    }

    /**
     * Connect to a LiveKit room.
     *
     * @param url   WebSocket URL of the LiveKit server
     * @param token Access token for authentication
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Room> connect(String url, String token) {
        connectFuture = new CompletableFuture<>();
        signalClient.connect(url, token);
        return connectFuture;
    }

    /**
     * Disconnect from the room.
     */
    public void disconnect() {
        signalClient.sendLeave();
        signalClient.disconnect();
        room.disconnect();
    }

    /**
     * Get the room instance.
     */
    public Room getRoom() {
        return room;
    }

    /**
     * Get the signal client for advanced operations.
     */
    public SignalClient getSignalClient() {
        return signalClient;
    }

    /**
     * Shutdown the client and release resources.
     */
    public void shutdown() {
        disconnect();
        signalClient.shutdown();
    }

    // SignalListener implementation - delegates to RoomSignalHandler

    @Override
    public void onStateChanged(SignalState state) {
        signalHandler.onStateChanged(state);
    }

    @Override
    public void onJoinResponse(LivekitRtc.JoinResponse response) {
        signalHandler.onJoinResponse(response);
        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.complete(room);
        }
    }

    @Override
    public void onAnswer(LivekitRtc.SessionDescription answer) {
        signalHandler.onAnswer(answer);
    }

    @Override
    public void onOffer(LivekitRtc.SessionDescription offer) {
        signalHandler.onOffer(offer);
    }

    @Override
    public void onTrickle(LivekitRtc.TrickleRequest trickle) {
        signalHandler.onTrickle(trickle);
    }

    @Override
    public void onParticipantUpdate(LivekitRtc.ParticipantUpdate update) {
        signalHandler.onParticipantUpdate(update);
    }

    @Override
    public void onTrackPublished(LivekitRtc.TrackPublishedResponse response) {
        signalHandler.onTrackPublished(response);
    }

    @Override
    public void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response) {
        signalHandler.onTrackUnpublished(response);
    }

    @Override
    public void onLeave(LivekitRtc.LeaveRequest leave) {
        signalHandler.onLeave(leave);
    }

    @Override
    public void onMuteTrack(LivekitRtc.MuteTrackRequest mute) {
        signalHandler.onMuteTrack(mute);
    }

    @Override
    public void onSpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged) {
        signalHandler.onSpeakersChanged(speakersChanged);
    }

    @Override
    public void onRoomUpdate(LivekitRtc.RoomUpdate roomUpdate) {
        signalHandler.onRoomUpdate(roomUpdate);
    }

    @Override
    public void onConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality) {
        signalHandler.onConnectionQuality(quality);
    }

    @Override
    public void onStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState) {
        signalHandler.onStreamStateUpdate(streamState);
    }

    @Override
    public void onRefreshToken(String token) {
        signalHandler.onRefreshToken(token);
    }

    @Override
    public void onReconnectResponse(LivekitRtc.ReconnectResponse response) {
        signalHandler.onReconnectResponse(response);
    }

    @Override
    public void onPong(long timestamp) {
        signalHandler.onPong(timestamp);
    }

    @Override
    public void onError(Exception e) {
        signalHandler.onError(e);
        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.completeExceptionally(e);
        }
    }

    @Override
    public void onIceServersUpdated(List<LivekitRtc.ICEServer> iceServers) {
        signalHandler.onIceServersUpdated(iceServers);
    }

    @Override
    public void onIceRestartRequired(ReconnectReason reason) {
        signalHandler.onIceRestartRequired(reason);
    }
}
