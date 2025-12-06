package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomSignalHandler;
import io.livekit.sdk.signaling.ReconnectReason;
import io.livekit.sdk.signaling.SignalClient;
import io.livekit.sdk.signaling.SignalListener;
import io.livekit.sdk.signaling.SignalState;
import livekit.LivekitRtc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main client that coordinates Room, SignalClient, and RtcEngine.
 * This is the primary entry point for connecting to a LiveKit room with full media support.
 */
public class RtcClient implements SignalListener, RtcEngineListener {

    private final Room room;
    private final SignalClient signalClient;
    private final RoomSignalHandler signalHandler;
    private final PeerConnectionEngine rtcEngine;

    private CompletableFuture<Room> connectFuture;
    private boolean hasPublishedTracks = false;

    public RtcClient() {
        this(new RoomOptions());
    }

    public RtcClient(RoomOptions options) {
        this.room = new Room(options);
        this.signalClient = new SignalClient();
        this.signalHandler = room.getSignalHandler();
        this.rtcEngine = new PeerConnectionEngine();

        this.signalClient.addListener(this);
        this.rtcEngine.setListener(this);
    }

    /**
     * Connect to a LiveKit room.
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
        rtcEngine.close();
        room.disconnect();
    }

    /**
     * Publish a local audio track.
     */
    public void publishAudioTrack(LocalAudioTrack track) {
        rtcEngine.addAudioTrack(track);
        hasPublishedTracks = true;
    }

    /**
     * Publish a local video track.
     */
    public void publishVideoTrack(LocalVideoTrack track) {
        rtcEngine.addVideoTrack(track);
        hasPublishedTracks = true;
    }

    /**
     * Unpublish a track.
     */
    public void unpublishTrack(String trackId) {
        rtcEngine.removeTrack(trackId);
    }

    public Room getRoom() {
        return room;
    }

    public SignalClient getSignalClient() {
        return signalClient;
    }

    public PeerConnectionEngine getRtcEngine() {
        return rtcEngine;
    }

    public void shutdown() {
        disconnect();
        signalClient.shutdown();
    }

    // SignalListener implementation

    @Override
    public void onStateChanged(SignalState state) {
        signalHandler.onStateChanged(state);
    }

    @Override
    public void onJoinResponse(LivekitRtc.JoinResponse response) {
        // Initialize RTC engine with ICE servers from join response
        List<IceServerConfig> iceServers = convertIceServers(response.getIceServersList());
        rtcEngine.initialize(iceServers);

        signalHandler.onJoinResponse(response);

        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.complete(room);
        }
    }

    @Override
    public void onAnswer(LivekitRtc.SessionDescription answer) {
        RTCSessionDescription rtcAnswer = new RTCSessionDescription(
                RTCSdpType.ANSWER,
                answer.getSdp()
        );
        rtcEngine.setPublisherAnswer(rtcAnswer, new RtcEngine.SdpCallback() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                // Answer set successfully
            }

            @Override
            public void onFailure(String error) {
                onError("Failed to set answer: " + error);
            }
        });
    }

    @Override
    public void onOffer(LivekitRtc.SessionDescription offer) {
        RTCSessionDescription rtcOffer = new RTCSessionDescription(
                RTCSdpType.OFFER,
                offer.getSdp()
        );
        rtcEngine.handleSubscriberOffer(rtcOffer, new RtcEngine.SdpCallback() {
            @Override
            public void onSuccess(RTCSessionDescription answer) {
                LivekitRtc.SessionDescription sdpAnswer = LivekitRtc.SessionDescription.newBuilder()
                        .setType("answer")
                        .setSdp(answer.sdp)
                        .build();
                signalClient.sendAnswer(sdpAnswer);
            }

            @Override
            public void onFailure(String error) {
                onError("Failed to handle offer: " + error);
            }
        });
    }

    @Override
    public void onTrickle(LivekitRtc.TrickleRequest trickle) {
        // candidateInit is a JSON string, parse it
        RTCIceCandidate candidate = IceCandidateParser.parse(trickle.getCandidateInit());
        if (candidate == null) {
            return;
        }
        int target = trickle.getTarget() == LivekitRtc.SignalTarget.PUBLISHER
                ? RtcEngine.TARGET_PUBLISHER
                : RtcEngine.TARGET_SUBSCRIBER;
        rtcEngine.addIceCandidate(candidate, target);
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
        rtcEngine.close();
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
        // Update ICE servers if provided
        if (response.getIceServersCount() > 0) {
            List<IceServerConfig> iceServers = convertIceServers(response.getIceServersList());
            rtcEngine.updateIceServers(iceServers);
        }
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
        List<IceServerConfig> configs = convertIceServers(iceServers);
        rtcEngine.updateIceServers(configs);
    }

    @Override
    public void onIceRestartRequired(ReconnectReason reason) {
        rtcEngine.restartPublisherIce();
        rtcEngine.restartSubscriberIce();
    }

    // RtcEngineListener implementation

    @Override
    public void onIceCandidate(RTCIceCandidate candidate, int target) {
        // candidateInit is a JSON string
        String candidateJson = IceCandidateParser.toJson(candidate);
        LivekitRtc.TrickleRequest trickle = LivekitRtc.TrickleRequest.newBuilder()
                .setTarget(target == RtcEngine.TARGET_PUBLISHER
                        ? LivekitRtc.SignalTarget.PUBLISHER
                        : LivekitRtc.SignalTarget.SUBSCRIBER)
                .setCandidateInit(candidateJson)
                .build();
        signalClient.sendTrickle(trickle);
    }

    @Override
    public void onPublisherIceConnectionChange(RTCIceConnectionState state) {
        if (state == RTCIceConnectionState.FAILED) {
            signalClient.requestIceRestart(ReconnectReason.PUBLISHER_FAILED);
        }
    }

    @Override
    public void onSubscriberIceConnectionChange(RTCIceConnectionState state) {
        if (state == RTCIceConnectionState.FAILED) {
            signalClient.requestIceRestart(ReconnectReason.SUBSCRIBER_FAILED);
        }
    }

    @Override
    public void onRemoteTrackReceived(MediaStreamTrack track, String[] streamIds) {
        // TODO: Map to Room track subscriptions
    }

    @Override
    public void onRemoteTrackRemoved(MediaStreamTrack track) {
        // TODO: Handle track removal
    }

    @Override
    public void onDataReceived(byte[] data, boolean reliable) {
        // TODO: Forward to Room data handlers
    }

    @Override
    public void onPublisherNegotiationNeeded() {
        if (!hasPublishedTracks) {
            return;
        }

        rtcEngine.createPublisherOffer(new RtcEngine.SdpCallback() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                LivekitRtc.SessionDescription offer = LivekitRtc.SessionDescription.newBuilder()
                        .setType("offer")
                        .setSdp(description.sdp)
                        .build();
                signalClient.sendOffer(offer);
            }

            @Override
            public void onFailure(String error) {
                onError("Failed to create offer: " + error);
            }
        });
    }

    @Override
    public void onError(String error) {
        // Log or notify error
    }

    // Helper methods

    private List<IceServerConfig> convertIceServers(List<LivekitRtc.ICEServer> protoServers) {
        List<IceServerConfig> configs = new ArrayList<>();
        for (LivekitRtc.ICEServer server : protoServers) {
            IceServerConfig config = new IceServerConfig(server.getUrlsList());
            if (!server.getUsername().isEmpty()) {
                config.setUsername(server.getUsername());
            }
            if (!server.getCredential().isEmpty()) {
                config.setCredential(server.getCredential());
            }
            configs.add(config);
        }
        return configs;
    }
}
