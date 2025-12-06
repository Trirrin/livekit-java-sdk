package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import io.livekit.sdk.LocalParticipant;
import io.livekit.sdk.LocalTrackManager;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomSignalHandler;
import io.livekit.sdk.Track;
import io.livekit.sdk.signaling.ReconnectReason;
import io.livekit.sdk.signaling.SignalClient;
import io.livekit.sdk.signaling.SignalListener;
import io.livekit.sdk.signaling.SignalState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import livekit.LivekitModels;
import livekit.LivekitRtc;

/**
 * Main client that coordinates Room, SignalClient, and RtcEngine. This is the primary entry point
 * for connecting to a LiveKit room with full media support.
 */
public class RtcClient implements SignalListener, RtcEngineListener, LocalTrackManager {

  private final Room room;
  private final SignalClient signalClient;
  private final RoomSignalHandler signalHandler;
  private final PeerConnectionEngine rtcEngine;

  // Maps mid (media stream id from SDP) to track SID
  private final Map<String, String> midToTrackSid = new ConcurrentHashMap<>();
  // Maps track SID to the subscribed track
  private final Map<String, Track> subscribedTracks = new ConcurrentHashMap<>();
  // Published local tracks
  private LocalAudioTrack publishedAudioTrack;
  private LocalVideoTrack publishedVideoTrack;

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

  /** Connect to a LiveKit room. */
  public CompletableFuture<Room> connect(String url, String token) {
    connectFuture = new CompletableFuture<>();
    signalClient.connect(url, token);
    return connectFuture;
  }

  /** Disconnect from the room. */
  public void disconnect() {
    signalClient.sendLeave();
    signalClient.disconnect();
    rtcEngine.close();
    room.disconnect();
  }

  /** Publish a local audio track. */
  public void publishAudioTrack(LocalAudioTrack track) {
    publishedAudioTrack = track;
    rtcEngine.addAudioTrack(track);
    hasPublishedTracks = true;
  }

  /** Publish a local video track. */
  public void publishVideoTrack(LocalVideoTrack track) {
    publishedVideoTrack = track;
    rtcEngine.addVideoTrack(track);
    hasPublishedTracks = true;
  }

  /** Unpublish a track. */
  public void unpublishTrack(String trackId) {
    if (publishedAudioTrack != null && publishedAudioTrack.getId().equals(trackId)) {
      publishedAudioTrack = null;
    }
    if (publishedVideoTrack != null && publishedVideoTrack.getId().equals(trackId)) {
      publishedVideoTrack = null;
    }
    rtcEngine.removeTrack(trackId);
  }

  /** Send data to other participants. */
  public boolean publishData(io.livekit.sdk.DataPacket packet) {
    boolean reliable = packet.getKind() == io.livekit.sdk.DataPacket.Kind.RELIABLE;

    // Build protobuf DataPacket
    LivekitModels.UserPacket.Builder userBuilder =
        LivekitModels.UserPacket.newBuilder()
            .setPayload(com.google.protobuf.ByteString.copyFrom(packet.getData()));

    if (packet.getTopic() != null) {
      userBuilder.setTopic(packet.getTopic());
    }

    LivekitModels.DataPacket.Builder dataBuilder =
        LivekitModels.DataPacket.newBuilder()
            .setKind(
                reliable
                    ? LivekitModels.DataPacket.Kind.RELIABLE
                    : LivekitModels.DataPacket.Kind.LOSSY)
            .setUser(userBuilder.build());

    if (packet.getDestinationIdentities() != null) {
      dataBuilder.addAllDestinationIdentities(packet.getDestinationIdentities());
    }

    return rtcEngine.sendData(dataBuilder.build().toByteArray(), reliable);
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

  /** Get available audio input devices. */
  public java.util.List<MediaDeviceInfo> getAudioInputDevices() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.getAudioInputDevices() : java.util.Collections.emptyList();
  }

  /** Get available audio output devices. */
  public java.util.List<MediaDeviceInfo> getAudioOutputDevices() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.getAudioOutputDevices() : java.util.Collections.emptyList();
  }

  /** Get available video input devices. */
  public java.util.List<MediaDeviceInfo> getVideoInputDevices() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.getVideoInputDevices() : java.util.Collections.emptyList();
  }

  /** Create a local audio track using the default audio device. */
  public LocalAudioTrack createAudioTrack() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.createAudioTrack() : null;
  }

  /** Create a local audio track using a specific device. */
  public LocalAudioTrack createAudioTrack(String deviceId, String name) {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.createAudioTrack(deviceId, name) : null;
  }

  /** Create a local video track using the default video device. */
  public LocalVideoTrack createVideoTrack() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.createVideoTrack() : null;
  }

  /** Create a local video track with specified parameters. */
  public LocalVideoTrack createVideoTrack(
      String deviceId, String name, int width, int height, int frameRate) {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null
        ? helper.createVideoTrack(deviceId, name, width, height, frameRate)
        : null;
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

    // Register this client as the track manager for the local participant
    LocalParticipant localParticipant = room.getLocalParticipant();
    if (localParticipant != null) {
      localParticipant.setTrackManager(this);
    }

    if (connectFuture != null && !connectFuture.isDone()) {
      connectFuture.complete(room);
    }
  }

  @Override
  public void onAnswer(LivekitRtc.SessionDescription answer) {
    RTCSessionDescription rtcAnswer = new RTCSessionDescription(RTCSdpType.ANSWER, answer.getSdp());
    rtcEngine.setPublisherAnswer(
        rtcAnswer,
        new RtcEngine.SdpCallback() {
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
    // Store mid to track SID mapping from offer
    Map<String, String> midMap = offer.getMidToTrackIdMap();
    if (midMap != null && !midMap.isEmpty()) {
      midToTrackSid.putAll(midMap);
    }

    RTCSessionDescription rtcOffer = new RTCSessionDescription(RTCSdpType.OFFER, offer.getSdp());
    rtcEngine.handleSubscriberOffer(
        rtcOffer,
        new RtcEngine.SdpCallback() {
          @Override
          public void onSuccess(RTCSessionDescription answer) {
            LivekitRtc.SessionDescription sdpAnswer =
                LivekitRtc.SessionDescription.newBuilder()
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
    int target =
        trickle.getTarget() == LivekitRtc.SignalTarget.PUBLISHER
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
    String cid = response.getCid();
    String sid = response.getTrack().getSid();

    // Store SID in local track for mute signaling
    if (publishedAudioTrack != null && publishedAudioTrack.getId().equals(cid)) {
      publishedAudioTrack.setSid(sid);
    }
    if (publishedVideoTrack != null && publishedVideoTrack.getId().equals(cid)) {
      publishedVideoTrack.setSid(sid);
    }

    signalHandler.onTrackPublished(response);
  }

  @Override
  public void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response) {
    signalHandler.onTrackUnpublished(response);
  }

  @Override
  public void onLeave(LivekitRtc.LeaveRequest leave) {
    // Clean up local tracks
    if (publishedAudioTrack != null) {
      publishedAudioTrack.dispose();
      publishedAudioTrack = null;
    }
    if (publishedVideoTrack != null) {
      publishedVideoTrack.dispose();
      publishedVideoTrack = null;
    }

    rtcEngine.close();
    signalClient.disconnect();
    signalHandler.onLeave(leave);
  }

  @Override
  public void onMuteTrack(LivekitRtc.MuteTrackRequest mute) {
    String sid = mute.getSid();
    boolean muted = mute.getMuted();

    // Apply mute state to local track
    if (publishedAudioTrack != null && sid.equals(publishedAudioTrack.getSid())) {
      publishedAudioTrack.setMuted(muted);
    }
    if (publishedVideoTrack != null && sid.equals(publishedVideoTrack.getSid())) {
      publishedVideoTrack.setMuted(muted);
    }

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
    LivekitRtc.TrickleRequest trickle =
        LivekitRtc.TrickleRequest.newBuilder()
            .setTarget(
                target == RtcEngine.TARGET_PUBLISHER
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
  public void onRemoteTrackReceived(
      MediaStreamTrack nativeTrack, RTCRtpTransceiver transceiver, String[] streamIds) {
    String mid = transceiver.getMid();
    if (mid == null) {
      return;
    }

    String trackSid = midToTrackSid.get(mid);
    if (trackSid == null) {
      return;
    }

    // Create appropriate track wrapper based on media type
    Track sdkTrack;
    String kind = nativeTrack.getKind();
    if ("audio".equals(kind)) {
      sdkTrack =
          new RemoteAudioTrack(
              trackSid, trackSid, (dev.onvoid.webrtc.media.audio.AudioTrack) nativeTrack);
    } else if ("video".equals(kind)) {
      sdkTrack =
          new RemoteVideoTrack(
              trackSid, trackSid, (dev.onvoid.webrtc.media.video.VideoTrack) nativeTrack);
    } else {
      return;
    }

    subscribedTracks.put(trackSid, sdkTrack);
    room.onTrackSubscribed(trackSid, sdkTrack);
  }

  @Override
  public void onRemoteTrackRemoved(MediaStreamTrack nativeTrack) {
    // Find and remove the track by matching native track
    String removedSid = null;
    Track removedTrack = null;

    for (Map.Entry<String, Track> entry : subscribedTracks.entrySet()) {
      Track track = entry.getValue();
      boolean matches = false;
      if (track instanceof RemoteAudioTrack) {
        matches = ((RemoteAudioTrack) track).getNativeTrack() == nativeTrack;
      } else if (track instanceof RemoteVideoTrack) {
        matches = ((RemoteVideoTrack) track).getNativeTrack() == nativeTrack;
      }
      if (matches) {
        removedSid = entry.getKey();
        removedTrack = track;
        break;
      }
    }

    if (removedSid != null) {
      subscribedTracks.remove(removedSid);
      room.onTrackUnsubscribed(removedSid, removedTrack);
    }
  }

  @Override
  public void onDataReceived(byte[] data, boolean reliable) {
    try {
      LivekitModels.DataPacket packet = LivekitModels.DataPacket.parseFrom(data);
      io.livekit.sdk.DataPacket.Kind kind =
          reliable ? io.livekit.sdk.DataPacket.Kind.RELIABLE : io.livekit.sdk.DataPacket.Kind.LOSSY;

      String participantSid = packet.getParticipantSid();
      if (participantSid.isEmpty()) {
        participantSid = null;
      }

      // Handle UserPacket (the most common case)
      if (packet.hasUser()) {
        LivekitModels.UserPacket user = packet.getUser();
        byte[] payload = user.getPayload().toByteArray();
        String topic = user.hasTopic() ? user.getTopic() : null;
        room.onDataReceived(payload, kind, participantSid, topic);
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      onError("Failed to parse data packet: " + e.getMessage());
    }
  }

  @Override
  public void onPublisherNegotiationNeeded() {
    if (!hasPublishedTracks) {
      return;
    }

    rtcEngine.createPublisherOffer(
        new RtcEngine.SdpCallback() {
          @Override
          public void onSuccess(RTCSessionDescription description) {
            LivekitRtc.SessionDescription offer =
                LivekitRtc.SessionDescription.newBuilder()
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

  // LocalTrackManager implementation

  @Override
  public void setMicrophoneEnabled(boolean enabled) {
    if (publishedAudioTrack != null) {
      boolean muted = !enabled;
      publishedAudioTrack.setMuted(muted);

      // Send mute state to server
      String sid = publishedAudioTrack.getSid();
      if (sid != null) {
        LivekitRtc.MuteTrackRequest muteRequest =
            LivekitRtc.MuteTrackRequest.newBuilder().setSid(sid).setMuted(muted).build();
        signalClient.sendMuteTrack(muteRequest);
      }
    }
  }

  @Override
  public void setCameraEnabled(boolean enabled) {
    if (publishedVideoTrack != null) {
      boolean muted = !enabled;
      publishedVideoTrack.setMuted(muted);

      // Send mute state to server
      String sid = publishedVideoTrack.getSid();
      if (sid != null) {
        LivekitRtc.MuteTrackRequest muteRequest =
            LivekitRtc.MuteTrackRequest.newBuilder().setSid(sid).setMuted(muted).build();
        signalClient.sendMuteTrack(muteRequest);
      }
    }
  }

  @Override
  public void setScreenShareEnabled(boolean enabled) {
    // Screen share is not yet implemented - tracked in TODOS.md
  }

  @Override
  public boolean isMicrophoneEnabled() {
    return publishedAudioTrack != null && !publishedAudioTrack.isMuted();
  }

  @Override
  public boolean isCameraEnabled() {
    return publishedVideoTrack != null && !publishedVideoTrack.isMuted();
  }

  @Override
  public boolean isScreenShareEnabled() {
    // Screen share is not yet implemented
    return false;
  }
}
