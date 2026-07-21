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
public class RtcClient
    implements SignalListener, RtcEngineListener, LocalTrackManager, io.livekit.sdk.RoomTransport {

  private final Room room;
  private final SignalClient signalClient;
  private final RoomSignalHandler signalHandler;
  private final PeerConnectionEngine rtcEngine;

  // Maps mid (media stream id from SDP) to track SID
  private final Map<String, String> midToTrackSid = new ConcurrentHashMap<>();
  // Maps track SID to the subscribed track
  private final Map<String, Track> subscribedTracks = new ConcurrentHashMap<>();
  // Published local tracks, keyed by client-generated track id (cid)
  private final Map<String, PublishedTrack> publishedTracks = new ConcurrentHashMap<>();

  private CompletableFuture<Room> connectFuture;
  private boolean hasPublishedTracks = false;

  /** A published local track together with its source. */
  private static final class PublishedTrack {
    final LocalMediaTrack track;
    final LivekitModels.TrackSource source;

    PublishedTrack(LocalMediaTrack track, LivekitModels.TrackSource source) {
      this.track = track;
      this.source = source;
    }
  }

  public RtcClient() {
    this(new RoomOptions());
  }

  public RtcClient(RoomOptions options) {
    this.room = new Room(options);
    this.signalClient = new SignalClient();
    this.signalHandler = room.getSignalHandler();
    this.rtcEngine = new PeerConnectionEngine();

    this.signalClient.addListener(this);
    this.signalClient.setAutoSubscribe(options.isAutoSubscribe());
    this.signalClient.setMaxReconnectAttempts(options.getReconnectAttempts());
    this.signalClient.setReconnectDelayMs(options.getReconnectDelayMs());
    this.rtcEngine.setListener(this);
    this.room.setTransport(this);
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

  /** Publish a local audio track as microphone audio. */
  public void publishAudioTrack(LocalAudioTrack track) {
    publishAudioTrack(track, LivekitModels.TrackSource.MICROPHONE);
  }

  /** Publish a local audio track with an explicit source. */
  public void publishAudioTrack(LocalAudioTrack track, LivekitModels.TrackSource source) {
    publishedTracks.put(track.getId(), new PublishedTrack(track, source));

    // Send AddTrackRequest to server before adding to PeerConnection
    LivekitRtc.AddTrackRequest addTrack =
        LivekitRtc.AddTrackRequest.newBuilder()
            .setCid(track.getId())
            .setName(track.getName())
            .setType(LivekitModels.TrackType.AUDIO)
            .setSource(source)
            .build();
    signalClient.sendAddTrack(addTrack);

    rtcEngine.addAudioTrack(track);
    hasPublishedTracks = true;
  }

  /** Publish a local video track as camera video. */
  public void publishVideoTrack(LocalVideoTrack track) {
    publishVideoTrack(track, LivekitModels.TrackSource.CAMERA);
  }

  /** Publish a local video track with an explicit source (camera or screen share). */
  public void publishVideoTrack(LocalVideoTrack track, LivekitModels.TrackSource source) {
    publishedTracks.put(track.getId(), new PublishedTrack(track, source));

    // Send AddTrackRequest to server before adding to PeerConnection
    LivekitRtc.AddTrackRequest addTrack =
        LivekitRtc.AddTrackRequest.newBuilder()
            .setCid(track.getId())
            .setName(track.getName())
            .setType(LivekitModels.TrackType.VIDEO)
            .setSource(source)
            .setWidth(track.getWidth())
            .setHeight(track.getHeight())
            .build();
    signalClient.sendAddTrack(addTrack);

    rtcEngine.addVideoTrack(track);
    hasPublishedTracks = true;
  }

  /** Unpublish a track. */
  public void unpublishTrack(String trackId) {
    publishedTracks.remove(trackId);
    rtcEngine.removeTrack(trackId);
  }

  private PublishedTrack findBySource(LivekitModels.TrackSource source) {
    for (PublishedTrack published : publishedTracks.values()) {
      if (published.source == source) {
        return published;
      }
    }
    return null;
  }

  /** Send data to other participants. */
  public boolean publishData(io.livekit.sdk.DataPacket packet) {
    boolean reliable = packet.getKind() == io.livekit.sdk.DataPacket.Kind.RELIABLE;
    return sendDataPacket(io.livekit.sdk.ProtoConverter.buildUserDataPacket(packet), reliable);
  }

  // RoomTransport implementation

  @Override
  public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {
    signalClient.sendUpdateMetadata(metadata);
  }

  @Override
  public void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription) {
    signalClient.sendUpdateSubscription(subscription);
  }

  @Override
  public void sendUpdateTrackSettings(LivekitRtc.UpdateTrackSettings settings) {
    signalClient.sendUpdateTrackSettings(settings);
  }

  @Override
  public void sendSubscriptionPermission(LivekitRtc.SubscriptionPermission permission) {
    signalClient.sendSubscriptionPermission(permission);
  }

  @Override
  public void sendMuteTrack(String trackSid, boolean muted) {
    signalClient.sendMuteTrack(
        LivekitRtc.MuteTrackRequest.newBuilder().setSid(trackSid).setMuted(muted).build());
  }

  @Override
  public boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable) {
    return rtcEngine.sendData(packet.toByteArray(), reliable);
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

  /** Create a local audio track with explicit capture processing options. */
  public LocalAudioTrack createAudioTrack(
      String deviceId, String name, AudioCaptureOptions captureOptions) {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.createAudioTrack(deviceId, name, captureOptions) : null;
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
    PublishedTrack published = publishedTracks.get(cid);
    if (published != null) {
      published.track.setSid(sid);
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
    for (PublishedTrack published : publishedTracks.values()) {
      published.track.dispose();
    }
    publishedTracks.clear();

    rtcEngine.close();
    signalClient.disconnect();
    signalHandler.onLeave(leave);
  }

  @Override
  public void onMuteTrack(LivekitRtc.MuteTrackRequest mute) {
    String sid = mute.getSid();
    boolean muted = mute.getMuted();

    // Apply mute state to local track
    for (PublishedTrack published : publishedTracks.values()) {
      if (sid.equals(published.track.getSid())) {
        published.track.setMuted(muted);
      }
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
  public void onSubscriptionPermissionUpdate(LivekitRtc.SubscriptionPermissionUpdate update) {
    signalHandler.onSubscriptionPermissionUpdate(update);
  }

  @Override
  public void onSubscriptionResponse(LivekitRtc.SubscriptionResponse response) {
    signalHandler.onSubscriptionResponse(response);
  }

  @Override
  public void onRequestResponse(LivekitRtc.RequestResponse response) {
    signalHandler.onRequestResponse(response);
  }

  @Override
  public void onLocalTrackSubscribed(LivekitRtc.TrackSubscribed trackSubscribed) {
    signalHandler.onLocalTrackSubscribed(trackSubscribed);
  }

  @Override
  public void onRoomMoved(LivekitRtc.RoomMovedResponse moved) {
    signalHandler.onRoomMoved(moved);
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
      room.handleDataPacket(packet, kind);
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
    setSourceMuted(LivekitModels.TrackSource.MICROPHONE, !enabled);
  }

  @Override
  public void setCameraEnabled(boolean enabled) {
    setSourceMuted(LivekitModels.TrackSource.CAMERA, !enabled);
  }

  private void setSourceMuted(LivekitModels.TrackSource source, boolean muted) {
    PublishedTrack published = findBySource(source);
    if (published == null) {
      return;
    }
    published.track.setMuted(muted);

    // Send mute state to server
    String sid = published.track.getSid();
    if (sid != null) {
      sendMuteTrack(sid, muted);
    }
  }

  @Override
  public void setScreenShareEnabled(boolean enabled) {
    PublishedTrack existing = findBySource(LivekitModels.TrackSource.SCREEN_SHARE);
    if (enabled) {
      if (existing != null) {
        return;
      }
      MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
      if (helper == null) {
        return;
      }
      LocalVideoTrack track = helper.createScreenShareTrack();
      if (track != null) {
        publishVideoTrack(track, LivekitModels.TrackSource.SCREEN_SHARE);
      }
    } else {
      if (existing == null) {
        return;
      }
      unpublishTrack(existing.track.getId());
      existing.track.dispose();
    }
  }

  /** Publish a screen share track for a specific screen or window. */
  public void publishScreenShareTrack(DesktopSourceInfo source, int maxWidth, int maxHeight) {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    if (helper == null) {
      return;
    }
    LocalVideoTrack track =
        helper.createScreenShareTrack(source, "screen", maxWidth, maxHeight, 30);
    if (track != null) {
      publishVideoTrack(track, LivekitModels.TrackSource.SCREEN_SHARE);
    }
  }

  /** Get shareable screens. */
  public java.util.List<DesktopSourceInfo> getScreenSources() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.getScreenSources() : java.util.Collections.emptyList();
  }

  /** Get shareable application windows. */
  public java.util.List<DesktopSourceInfo> getWindowSources() {
    MediaDevicesHelper helper = rtcEngine.getMediaDevicesHelper();
    return helper != null ? helper.getWindowSources() : java.util.Collections.emptyList();
  }

  @Override
  public boolean isMicrophoneEnabled() {
    PublishedTrack published = findBySource(LivekitModels.TrackSource.MICROPHONE);
    return published != null && !published.track.isMuted();
  }

  @Override
  public boolean isCameraEnabled() {
    PublishedTrack published = findBySource(LivekitModels.TrackSource.CAMERA);
    return published != null && !published.track.isMuted();
  }

  @Override
  public boolean isScreenShareEnabled() {
    PublishedTrack published = findBySource(LivekitModels.TrackSource.SCREEN_SHARE);
    return published != null && !published.track.isMuted();
  }
}
