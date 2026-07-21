package io.livekit.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import livekit.LivekitModels;
import livekit.LivekitRtc;

/** Represents a LiveKit room. This is the main entry point for interacting with a LiveKit room. */
public class Room implements TrackSubscriptionHandler {
  private String sid;
  private String name;
  private String metadata;
  private ConnectionState state;
  private LocalParticipant localParticipant;
  private final Map<String, RemoteParticipant> remoteParticipants;
  private final List<RoomListener> listeners;
  private final RoomOptions options;
  private RoomSignalHandler signalHandler;
  private RoomTransport transport;
  private io.livekit.sdk.rpc.RpcManager rpcManager;
  private io.livekit.sdk.datastreams.DataStreamManager dataStreamManager;

  public Room() {
    this(new RoomOptions());
  }

  public Room(RoomOptions options) {
    this.options = options;
    this.state = ConnectionState.DISCONNECTED;
    this.remoteParticipants = new ConcurrentHashMap<>();
    this.listeners = new CopyOnWriteArrayList<>();
  }

  /** Get the signal handler for this room. Used by signaling module to integrate with the room. */
  public RoomSignalHandler getSignalHandler() {
    if (signalHandler == null) {
      signalHandler = new RoomSignalHandler(this);
    }
    return signalHandler;
  }

  /** Set the outbound transport. Called by the RTC layer that owns signaling and data channels. */
  public void setTransport(RoomTransport transport) {
    this.transport = transport;
    if (transport != null && rpcManager == null) {
      rpcManager = new io.livekit.sdk.rpc.RpcManager(transport);
    }
    if (transport != null && dataStreamManager == null) {
      dataStreamManager = new io.livekit.sdk.datastreams.DataStreamManager(transport);
    }
    if (localParticipant != null) {
      localParticipant.setTransport(transport);
      localParticipant.setRpcManager(rpcManager);
      localParticipant.setDataStreamManager(dataStreamManager);
    }
  }

  /** Get the RPC manager. Available once a transport has been set. */
  public io.livekit.sdk.rpc.RpcManager getRpcManager() {
    return rpcManager;
  }

  /** Get the data stream manager. Available once a transport has been set. */
  public io.livekit.sdk.datastreams.DataStreamManager getDataStreamManager() {
    return dataStreamManager;
  }

  /** Register a handler for incoming text streams on a topic. */
  public void registerTextStreamHandler(
      String topic, io.livekit.sdk.datastreams.TextStreamHandler handler) {
    requireDataStreamManager().registerTextStreamHandler(topic, handler);
  }

  public void unregisterTextStreamHandler(String topic) {
    if (dataStreamManager != null) {
      dataStreamManager.unregisterTextStreamHandler(topic);
    }
  }

  /** Register a handler for incoming byte streams on a topic. */
  public void registerByteStreamHandler(
      String topic, io.livekit.sdk.datastreams.ByteStreamHandler handler) {
    requireDataStreamManager().registerByteStreamHandler(topic, handler);
  }

  public void unregisterByteStreamHandler(String topic) {
    if (dataStreamManager != null) {
      dataStreamManager.unregisterByteStreamHandler(topic);
    }
  }

  private io.livekit.sdk.datastreams.DataStreamManager requireDataStreamManager() {
    if (dataStreamManager == null) {
      throw new IllegalStateException("No transport available; connect via RtcClient");
    }
    return dataStreamManager;
  }

  public RoomTransport getTransport() {
    return transport;
  }

  /**
   * Connect to a LiveKit room.
   *
   * @param url WebSocket URL of the LiveKit server
   * @param token Access token for authentication
   */
  public void connect(String url, String token) {
    if (state != ConnectionState.DISCONNECTED) {
      throw new IllegalStateException("Already connected or connecting");
    }
    setState(ConnectionState.CONNECTING);
    // Actual connection is handled by LiveKitClient which coordinates Room + SignalClient
  }

  /** Disconnect from the room. */
  public void disconnect() {
    if (state == ConnectionState.DISCONNECTED) {
      return;
    }
    setState(ConnectionState.DISCONNECTED);
    if (rpcManager != null) {
      rpcManager.failAllPending();
    }
    clearParticipants();
    notifyDisconnected(DisconnectReason.CLIENT_INITIATED);
  }

  /**
   * Send data to other participants.
   *
   * @return true if the packet was handed to an open data channel
   */
  public boolean publishData(DataPacket packet) {
    if (state != ConnectionState.CONNECTED) {
      throw new IllegalStateException("Not connected");
    }
    if (transport == null) {
      throw new IllegalStateException("No transport available; connect via RtcClient");
    }
    boolean reliable = packet.getKind() == DataPacket.Kind.RELIABLE;
    return transport.sendDataPacket(ProtoConverter.buildUserDataPacket(packet), reliable);
  }

  void clearParticipants() {
    remoteParticipants.clear();
    localParticipant = null;
  }

  // Signal handler callbacks for processing server messages
  void handleJoinResponse(LivekitRtc.JoinResponse response) {
    this.sid = response.getRoom().getSid();
    this.name = response.getRoom().getName();
    this.metadata = response.getRoom().getMetadata();

    // Create local participant
    this.localParticipant = ProtoConverter.localParticipantFromProto(response.getParticipant());
    this.localParticipant.setTransport(transport);
    this.localParticipant.setRpcManager(rpcManager);
    this.localParticipant.setDataStreamManager(dataStreamManager);

    // Add local participant tracks
    for (LivekitModels.TrackInfo trackInfo : response.getParticipant().getTracksList()) {
      TrackPublication pub = ProtoConverter.trackPublicationFromProto(trackInfo);
      localParticipant.addTrackPublication(pub);
    }

    // Process other participants
    for (LivekitModels.ParticipantInfo info : response.getOtherParticipantsList()) {
      handleParticipantInfo(info);
    }

    setState(ConnectionState.CONNECTED);
    notifyConnected();
  }

  void handleParticipantUpdate(LivekitRtc.ParticipantUpdate update) {
    for (LivekitModels.ParticipantInfo info : update.getParticipantsList()) {
      handleParticipantInfo(info);
    }
  }

  private void handleParticipantInfo(LivekitModels.ParticipantInfo info) {
    // Skip if this is us
    if (localParticipant != null && info.getSid().equals(localParticipant.getSid())) {
      applyParticipantUpdate(localParticipant, info);
      syncParticipantTracks(localParticipant, info.getTracksList());
      return;
    }

    RemoteParticipant participant = remoteParticipants.get(info.getIdentity());

    if (info.getState() == LivekitModels.ParticipantInfo.State.DISCONNECTED) {
      // Participant left
      if (participant != null) {
        removeRemoteParticipant(info.getIdentity());
      }
      return;
    }

    boolean isNew = (participant == null);
    if (isNew) {
      participant = ProtoConverter.remoteParticipantFromProto(info);
      remoteParticipants.put(info.getIdentity(), participant);
    } else {
      applyParticipantUpdate(participant, info);
    }

    syncParticipantTracks(participant, info.getTracksList());

    if (isNew) {
      notifyParticipantConnected(participant);
    }
  }

  /** Apply a participant info update to an existing participant, firing change events. */
  private void applyParticipantUpdate(Participant participant, LivekitModels.ParticipantInfo info) {
    String prevMetadata = participant.getMetadata();
    String prevName = participant.getName();
    Map<String, String> prevAttributes = new java.util.HashMap<>(participant.getAttributes());

    ProtoConverter.updateParticipantFromProto(participant, info);

    if (!java.util.Objects.equals(prevMetadata, participant.getMetadata())) {
      notifyParticipantMetadataChanged(participant, prevMetadata);
    }
    if (!java.util.Objects.equals(prevName, participant.getName())) {
      notifyParticipantNameChanged(participant, prevName);
    }
    if (!participant.getAttributes().equals(prevAttributes)) {
      notifyParticipantAttributesChanged(participant, prevAttributes);
    }
  }

  private void syncParticipantTracks(
      Participant participant, List<LivekitModels.TrackInfo> trackInfos) {
    Map<String, TrackPublication> currentPubs = participant.getTrackPublications();

    // Track which sids we've seen
    java.util.Set<String> seenSids = new java.util.HashSet<>();

    for (LivekitModels.TrackInfo info : trackInfos) {
      seenSids.add(info.getSid());

      TrackPublication existing = currentPubs.get(info.getSid());
      if (existing != null) {
        boolean wasMuted = existing.isMuted();
        ProtoConverter.updateTrackPublicationFromProto(existing, info);
        if (wasMuted != existing.isMuted()) {
          if (existing.isMuted()) {
            notifyTrackMuted(existing, participant);
          } else {
            notifyTrackUnmuted(existing, participant);
          }
        }
      } else {
        TrackPublication pub;
        if (participant instanceof RemoteParticipant) {
          RemoteTrackPublication remotePub = ProtoConverter.remoteTrackPublicationFromProto(info);
          remotePub.attach(transport, participant.getSid());
          pub = remotePub;
        } else {
          pub = ProtoConverter.trackPublicationFromProto(info);
        }
        participant.addTrackPublication(pub);
        notifyTrackPublished(pub, participant);
      }
    }

    // Remove tracks no longer present
    List<String> toRemove = new ArrayList<>();
    for (String sid : currentPubs.keySet()) {
      if (!seenSids.contains(sid)) {
        toRemove.add(sid);
      }
    }
    for (String sid : toRemove) {
      TrackPublication pub = currentPubs.get(sid);
      participant.removeTrackPublication(sid);
      notifyTrackUnpublished(pub, participant);
    }
  }

  void handleRoomUpdate(LivekitRtc.RoomUpdate update) {
    LivekitModels.Room room = update.getRoom();
    this.sid = room.getSid();
    this.name = room.getName();
    String prevMetadata = this.metadata;
    this.metadata = room.getMetadata();
    if (prevMetadata != null && !prevMetadata.equals(this.metadata)) {
      notifyRoomMetadataChanged(this.metadata);
    }
  }

  void handleSpeakersChanged(LivekitRtc.SpeakersChanged changed) {
    List<Participant> activeSpeakers = new ArrayList<>();
    for (LivekitModels.SpeakerInfo speaker : changed.getSpeakersList()) {
      Participant p = findParticipantBySid(speaker.getSid());
      if (p != null) {
        p.setSpeaking(speaker.getActive());
        p.setAudioLevel((long) (speaker.getLevel() * 100));
        if (speaker.getActive()) {
          activeSpeakers.add(p);
        }
      }
    }
    notifyActiveSpeakersChanged(activeSpeakers);
  }

  void handleConnectionQualityUpdate(LivekitRtc.ConnectionQualityUpdate update) {
    for (LivekitRtc.ConnectionQualityInfo info : update.getUpdatesList()) {
      Participant p = findParticipantBySid(info.getParticipantSid());
      if (p != null) {
        ConnectionQuality quality = ProtoConverter.fromProto(info.getQuality());
        p.setConnectionQuality(quality);
        notifyConnectionQualityChanged(p, quality);
      }
    }
  }

  void handleMuteTrack(LivekitRtc.MuteTrackRequest mute) {
    if (localParticipant == null) return;
    TrackPublication pub = localParticipant.getTrackPublication(mute.getSid());
    if (pub != null) {
      pub.setMuted(mute.getMuted());
      if (mute.getMuted()) {
        notifyTrackMuted(pub, localParticipant);
      } else {
        notifyTrackUnmuted(pub, localParticipant);
      }
    }
  }

  void handleLeave(LivekitRtc.LeaveRequest leave) {
    DisconnectReason reason = ProtoConverter.fromProto(leave.getReason());
    setState(ConnectionState.DISCONNECTED);
    if (rpcManager != null) {
      rpcManager.failAllPending();
    }
    clearParticipants();
    notifyDisconnected(reason);
  }

  void handleStreamStateUpdate(LivekitRtc.StreamStateUpdate update) {
    for (LivekitRtc.StreamStateInfo info : update.getStreamStatesList()) {
      Participant participant = findParticipantBySid(info.getParticipantSid());
      if (participant == null) {
        continue;
      }
      TrackPublication pub = participant.getTrackPublication(info.getTrackSid());
      if (pub instanceof RemoteTrackPublication) {
        RemoteTrackPublication remotePub = (RemoteTrackPublication) pub;
        TrackStreamState state = TrackStreamState.fromProto(info.getState());
        remotePub.setStreamState(state);
        notifyTrackStreamStateChanged(remotePub, state, participant);
      }
    }
  }

  void handleSubscriptionPermissionUpdate(LivekitRtc.SubscriptionPermissionUpdate update) {
    Participant participant = findParticipantBySid(update.getParticipantSid());
    if (participant == null) {
      return;
    }
    TrackPublication pub = participant.getTrackPublication(update.getTrackSid());
    if (pub instanceof RemoteTrackPublication) {
      RemoteTrackPublication remotePub = (RemoteTrackPublication) pub;
      remotePub.setSubscriptionAllowed(update.getAllowed());
      notifyTrackSubscriptionPermissionChanged(remotePub, participant, update.getAllowed());
    }
  }

  void handleSubscriptionResponse(LivekitRtc.SubscriptionResponse response) {
    notifyTrackSubscriptionFailed(response.getTrackSid(), response.getErr().name());
  }

  void handleRequestResponse(LivekitRtc.RequestResponse response) {
    if (response.getReason() != LivekitRtc.RequestResponse.Reason.OK) {
      notifySignalRequestError(
          response.getRequestId(), response.getReason().name(), response.getMessage());
    }
  }

  void handleLocalTrackSubscribed(String trackSid) {
    if (localParticipant == null) {
      return;
    }
    TrackPublication pub = localParticipant.getTrackPublication(trackSid);
    if (pub != null) {
      notifyLocalTrackSubscribed(pub);
    }
  }

  void handleRoomMoved(LivekitRtc.RoomMovedResponse moved) {
    this.sid = moved.getRoom().getSid();
    this.name = moved.getRoom().getName();
    this.metadata = moved.getRoom().getMetadata();

    // Re-sync participant state for the new room
    clearRemoteParticipants();
    if (moved.hasParticipant() && localParticipant != null) {
      applyParticipantUpdate(localParticipant, moved.getParticipant());
    }
    for (LivekitModels.ParticipantInfo info : moved.getOtherParticipantsList()) {
      handleParticipantInfo(info);
    }
    notifyRoomMoved();
  }

  private void clearRemoteParticipants() {
    remoteParticipants.clear();
  }

  void handleReconnecting() {
    setState(ConnectionState.RECONNECTING);
    notifyReconnecting();
  }

  void handleReconnected() {
    setState(ConnectionState.CONNECTED);
    notifyReconnected();
  }

  void handleSignalError(Exception e) {
    // Could implement error callback
  }

  private Participant findParticipantBySid(String sid) {
    if (localParticipant != null && localParticipant.getSid().equals(sid)) {
      return localParticipant;
    }
    for (RemoteParticipant p : remoteParticipants.values()) {
      if (p.getSid().equals(sid)) {
        return p;
      }
    }
    return null;
  }

  public String getSid() {
    return sid;
  }

  void setSid(String sid) {
    this.sid = sid;
  }

  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  public String getMetadata() {
    return metadata;
  }

  void setMetadata(String metadata) {
    String prev = this.metadata;
    this.metadata = metadata;
    if (prev != null && !prev.equals(metadata)) {
      notifyRoomMetadataChanged(metadata);
    }
  }

  public ConnectionState getState() {
    return state;
  }

  void setState(ConnectionState state) {
    this.state = state;
  }

  public LocalParticipant getLocalParticipant() {
    return localParticipant;
  }

  void setLocalParticipant(LocalParticipant localParticipant) {
    this.localParticipant = localParticipant;
  }

  public Map<String, RemoteParticipant> getRemoteParticipants() {
    return Collections.unmodifiableMap(remoteParticipants);
  }

  public RemoteParticipant getRemoteParticipant(String identity) {
    return remoteParticipants.get(identity);
  }

  void addRemoteParticipant(RemoteParticipant participant) {
    remoteParticipants.put(participant.getIdentity(), participant);
    notifyParticipantConnected(participant);
  }

  void removeRemoteParticipant(String identity) {
    RemoteParticipant participant = remoteParticipants.remove(identity);
    if (participant != null) {
      if (rpcManager != null) {
        rpcManager.handleParticipantDisconnected(identity);
      }
      notifyParticipantDisconnected(participant);
    }
  }

  public RoomOptions getOptions() {
    return options;
  }

  // Listener management
  public void addListener(RoomListener listener) {
    listeners.add(listener);
  }

  public void removeListener(RoomListener listener) {
    listeners.remove(listener);
  }

  // Event notification methods
  void notifyConnected() {
    for (RoomListener listener : listeners) {
      listener.onConnected(this);
    }
  }

  void notifyDisconnected(DisconnectReason reason) {
    for (RoomListener listener : listeners) {
      listener.onDisconnected(this, reason);
    }
  }

  void notifyReconnecting() {
    for (RoomListener listener : listeners) {
      listener.onReconnecting(this);
    }
  }

  void notifyReconnected() {
    for (RoomListener listener : listeners) {
      listener.onReconnected(this);
    }
  }

  void notifyParticipantConnected(RemoteParticipant participant) {
    for (RoomListener listener : listeners) {
      listener.onParticipantConnected(this, participant);
    }
  }

  void notifyParticipantDisconnected(RemoteParticipant participant) {
    for (RoomListener listener : listeners) {
      listener.onParticipantDisconnected(this, participant);
    }
  }

  void notifyTrackPublished(TrackPublication publication, Participant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackPublished(this, publication, participant);
    }
  }

  void notifyTrackUnpublished(TrackPublication publication, Participant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackUnpublished(this, publication, participant);
    }
  }

  void notifyTrackSubscribed(
      Track track, TrackPublication publication, RemoteParticipant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackSubscribed(this, track, publication, participant);
    }
  }

  void notifyTrackUnsubscribed(
      Track track, TrackPublication publication, RemoteParticipant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackUnsubscribed(this, track, publication, participant);
    }
  }

  void notifyTrackMuted(TrackPublication publication, Participant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackMuted(this, publication, participant);
    }
  }

  void notifyTrackUnmuted(TrackPublication publication, Participant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackUnmuted(this, publication, participant);
    }
  }

  void notifyDataReceived(
      byte[] data, RemoteParticipant participant, DataPacket.Kind kind, String topic) {
    for (RoomListener listener : listeners) {
      listener.onDataReceived(this, data, participant, kind, topic);
    }
  }

  void notifyActiveSpeakersChanged(List<Participant> speakers) {
    for (RoomListener listener : listeners) {
      listener.onActiveSpeakersChanged(this, speakers);
    }
  }

  void notifyConnectionQualityChanged(Participant participant, ConnectionQuality quality) {
    for (RoomListener listener : listeners) {
      listener.onConnectionQualityChanged(this, participant, quality);
    }
  }

  void notifyRoomMetadataChanged(String metadata) {
    for (RoomListener listener : listeners) {
      listener.onRoomMetadataChanged(this, metadata);
    }
  }

  void notifyParticipantMetadataChanged(Participant participant, String prevMetadata) {
    for (RoomListener listener : listeners) {
      listener.onParticipantMetadataChanged(this, participant, prevMetadata);
    }
  }

  void notifyParticipantNameChanged(Participant participant, String prevName) {
    for (RoomListener listener : listeners) {
      listener.onParticipantNameChanged(this, participant, prevName);
    }
  }

  void notifyParticipantAttributesChanged(
      Participant participant, Map<String, String> prevAttributes) {
    for (RoomListener listener : listeners) {
      listener.onParticipantAttributesChanged(this, participant, prevAttributes);
    }
  }

  void notifyTrackStreamStateChanged(
      RemoteTrackPublication publication, TrackStreamState state, Participant participant) {
    for (RoomListener listener : listeners) {
      listener.onTrackStreamStateChanged(this, publication, state, participant);
    }
  }

  void notifyTrackSubscriptionPermissionChanged(
      RemoteTrackPublication publication, Participant participant, boolean allowed) {
    for (RoomListener listener : listeners) {
      listener.onTrackSubscriptionPermissionChanged(this, publication, participant, allowed);
    }
  }

  void notifyTrackSubscriptionFailed(String trackSid, String error) {
    for (RoomListener listener : listeners) {
      listener.onTrackSubscriptionFailed(this, trackSid, error);
    }
  }

  void notifySignalRequestError(long requestId, String reason, String message) {
    for (RoomListener listener : listeners) {
      listener.onSignalRequestError(this, requestId, reason, message);
    }
  }

  void notifyLocalTrackSubscribed(TrackPublication publication) {
    for (RoomListener listener : listeners) {
      listener.onLocalTrackSubscribed(this, publication);
    }
  }

  void notifyRoomMoved() {
    for (RoomListener listener : listeners) {
      listener.onRoomMoved(this);
    }
  }

  // TrackSubscriptionHandler implementation

  @Override
  public void onTrackSubscribed(String trackSid, Track track) {
    // Find the publication and participant for this track
    for (RemoteParticipant participant : remoteParticipants.values()) {
      TrackPublication pub = participant.getTrackPublication(trackSid);
      if (pub != null) {
        pub.setTrack(track);
        pub.setSubscribed(true);
        notifyTrackSubscribed(track, pub, participant);
        return;
      }
    }
  }

  @Override
  public void onTrackUnsubscribed(String trackSid, Track track) {
    // Find the publication and participant for this track
    for (RemoteParticipant participant : remoteParticipants.values()) {
      TrackPublication pub = participant.getTrackPublication(trackSid);
      if (pub != null && pub.getTrack() == track) {
        pub.setTrack(null);
        pub.setSubscribed(false);
        notifyTrackUnsubscribed(track, pub, participant);
        return;
      }
    }
  }

  @Override
  public void onDataReceived(
      byte[] data, DataPacket.Kind kind, String participantSid, String topic) {
    RemoteParticipant sender = null;
    if (participantSid != null) {
      for (RemoteParticipant p : remoteParticipants.values()) {
        if (p.getSid().equals(participantSid)) {
          sender = p;
          break;
        }
      }
    }
    notifyDataReceived(data, sender, kind, topic);
  }

  /** Dispatch an incoming protobuf data packet from the RTC layer. */
  public void handleDataPacket(LivekitModels.DataPacket packet, DataPacket.Kind kind) {
    switch (packet.getValueCase()) {
      case USER:
        LivekitModels.UserPacket user = packet.getUser();
        RemoteParticipant sender = findSender(packet);
        String topic = user.hasTopic() ? user.getTopic() : null;
        notifyDataReceived(user.getPayload().toByteArray(), sender, kind, topic);
        break;
      case RPC_REQUEST:
        if (rpcManager != null) {
          rpcManager.handleRequest(packet.getParticipantIdentity(), packet.getRpcRequest());
        }
        break;
      case RPC_ACK:
        if (rpcManager != null) {
          rpcManager.handleAck(packet.getRpcAck());
        }
        break;
      case RPC_RESPONSE:
        if (rpcManager != null) {
          rpcManager.handleResponse(packet.getRpcResponse());
        }
        break;
      case STREAM_HEADER:
        if (dataStreamManager != null) {
          dataStreamManager.handleHeader(packet.getStreamHeader(), packet.getParticipantIdentity());
        }
        break;
      case STREAM_CHUNK:
        if (dataStreamManager != null) {
          dataStreamManager.handleChunk(packet.getStreamChunk());
        }
        break;
      case STREAM_TRAILER:
        if (dataStreamManager != null) {
          dataStreamManager.handleTrailer(packet.getStreamTrailer());
        }
        break;
      default:
        break;
    }
  }

  private RemoteParticipant findSender(LivekitModels.DataPacket packet) {
    if (!packet.getParticipantIdentity().isEmpty()) {
      RemoteParticipant byIdentity = remoteParticipants.get(packet.getParticipantIdentity());
      if (byIdentity != null) {
        return byIdentity;
      }
    }
    if (!packet.getParticipantSid().isEmpty()) {
      for (RemoteParticipant p : remoteParticipants.values()) {
        if (p.getSid().equals(packet.getParticipantSid())) {
          return p;
        }
      }
    }
    return null;
  }
}
