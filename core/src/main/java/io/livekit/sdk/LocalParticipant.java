package io.livekit.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Represents the local participant (current user) in the room. */
public class LocalParticipant extends Participant {
  private final Map<String, TrackPublication> localTrackPublications;
  private LocalTrackManager trackManager;
  private RoomTransport transport;
  private io.livekit.sdk.rpc.RpcManager rpcManager;
  private io.livekit.sdk.datastreams.DataStreamManager dataStreamManager;

  public LocalParticipant(String sid, String identity) {
    super(sid, identity);
    this.localTrackPublications = new ConcurrentHashMap<>();
  }

  /** Set the track manager for controlling local media tracks. */
  public void setTrackManager(LocalTrackManager trackManager) {
    this.trackManager = trackManager;
  }

  void setTransport(RoomTransport transport) {
    this.transport = transport;
  }

  RoomTransport getTransport() {
    return transport;
  }

  void setRpcManager(io.livekit.sdk.rpc.RpcManager rpcManager) {
    this.rpcManager = rpcManager;
  }

  void setDataStreamManager(io.livekit.sdk.datastreams.DataStreamManager dataStreamManager) {
    this.dataStreamManager = dataStreamManager;
  }

  private io.livekit.sdk.datastreams.DataStreamManager requireDataStreamManager() {
    if (dataStreamManager == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    return dataStreamManager;
  }

  /** Send a complete text as a data stream. */
  public io.livekit.sdk.datastreams.TextStreamInfo sendText(
      String text, io.livekit.sdk.datastreams.StreamTextOptions options) {
    return requireDataStreamManager().sendText(text, options);
  }

  /** Open an incremental text stream. */
  public io.livekit.sdk.datastreams.TextStreamWriter streamText(
      io.livekit.sdk.datastreams.StreamTextOptions options) {
    return requireDataStreamManager().streamText(options);
  }

  /** Send a complete byte array as a data stream. */
  public io.livekit.sdk.datastreams.ByteStreamInfo sendBytes(
      byte[] data, io.livekit.sdk.datastreams.StreamByteOptions options) {
    return requireDataStreamManager().sendBytes(data, options);
  }

  /** Send a file as a byte data stream. Name and mime type are derived from the file. */
  public io.livekit.sdk.datastreams.ByteStreamInfo sendFile(
      java.io.File file, io.livekit.sdk.datastreams.StreamByteOptions options)
      throws java.io.IOException {
    byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
    options.setName(file.getName());
    String mimeType = java.nio.file.Files.probeContentType(file.toPath());
    if (mimeType != null) {
      options.setMimeType(mimeType);
    }
    return requireDataStreamManager().sendBytes(data, options);
  }

  /** Open an incremental byte stream. */
  public io.livekit.sdk.datastreams.ByteStreamWriter streamBytes(
      io.livekit.sdk.datastreams.StreamByteOptions options) {
    return requireDataStreamManager().streamBytes(options);
  }

  /** Send a chat message to all participants. */
  public ChatMessage sendChatMessage(String text) {
    ChatMessage message =
        new ChatMessage(
            java.util.UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            text,
            null,
            false,
            false);
    sendChatMessagePacket(message);
    return message;
  }

  /** Edit a previously sent chat message, keeping its id. */
  public ChatMessage editChatMessage(String newText, ChatMessage original) {
    ChatMessage edited =
        new ChatMessage(
            original.getId(),
            original.getTimestamp(),
            newText,
            System.currentTimeMillis(),
            original.isDeleted(),
            original.isGenerated());
    sendChatMessagePacket(edited);
    return edited;
  }

  private void sendChatMessagePacket(ChatMessage message) {
    if (transport == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    livekit.LivekitModels.DataPacket packet =
        livekit.LivekitModels.DataPacket.newBuilder().setChatMessage(message.toProto()).build();
    transport.sendDataPacket(packet, true);
  }

  private io.livekit.sdk.rpc.RpcManager requireRpcManager() {
    if (rpcManager == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    return rpcManager;
  }

  /** Invoke an RPC method on a remote participant with the default 10s response timeout. */
  public java.util.concurrent.CompletableFuture<String> performRpc(
      String destinationIdentity, String method, String payload) {
    return requireRpcManager().performRpc(destinationIdentity, method, payload);
  }

  /** Invoke an RPC method on a remote participant. */
  public java.util.concurrent.CompletableFuture<String> performRpc(
      String destinationIdentity, String method, String payload, long responseTimeoutMs) {
    return requireRpcManager().performRpc(destinationIdentity, method, payload, responseTimeoutMs);
  }

  /** Register a handler for an RPC method that remote participants can invoke. */
  public void registerRpcMethod(String method, io.livekit.sdk.rpc.RpcHandler handler) {
    requireRpcManager().registerMethod(method, handler);
  }

  /** Unregister a previously registered RPC method. */
  public void unregisterRpcMethod(String method) {
    if (rpcManager != null) {
      rpcManager.unregisterMethod(method);
    }
  }

  /** Get the track manager. */
  public LocalTrackManager getTrackManager() {
    return trackManager;
  }

  /**
   * Publish a local track to the room. This is a placeholder - actual implementation requires RTC
   * transport.
   */
  public void publishTrack(Track track) {
    // Will be implemented with RTC transport
  }

  /** Unpublish a local track. */
  public void unpublishTrack(Track track) {
    // Will be implemented with RTC transport
  }

  /** Set microphone enabled state. */
  public void setMicrophoneEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setMicrophoneEnabled(enabled);
    }
  }

  /** Set camera enabled state. */
  public void setCameraEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setCameraEnabled(enabled);
    }
  }

  /** Set screen share enabled state. */
  public void setScreenShareEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setScreenShareEnabled(enabled);
    }
  }

  /** Check if microphone is enabled. */
  public boolean isMicrophoneEnabled() {
    return trackManager != null && trackManager.isMicrophoneEnabled();
  }

  /** Check if camera is enabled. */
  public boolean isCameraEnabled() {
    return trackManager != null && trackManager.isCameraEnabled();
  }

  /** Check if screen share is enabled. */
  public boolean isScreenShareEnabled() {
    return trackManager != null && trackManager.isScreenShareEnabled();
  }

  /**
   * Update the local participant's metadata on the server. Requires the
   * canUpdateOwnParticipantMetadata permission. Local state is updated when the server confirms via
   * a participant update, which also fires {@code onParticipantMetadataChanged}.
   */
  public void updateMetadata(String metadata) {
    sendMetadataUpdate(metadata, null, null);
  }

  /** Update the local participant's display name on the server. */
  public void updateName(String name) {
    sendMetadataUpdate(null, name, null);
  }

  /**
   * Update the local participant's attributes on the server. Only the provided keys are updated; to
   * delete an attribute, set its value to an empty string.
   */
  public void updateAttributes(Map<String, String> attributes) {
    sendMetadataUpdate(null, null, attributes);
  }

  private void sendMetadataUpdate(String metadata, String name, Map<String, String> attributes) {
    if (transport == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    transport.sendUpdateMetadata(ProtoConverter.buildMetadataUpdate(metadata, name, attributes));
  }

  /**
   * Control who can subscribe to the local participant's published tracks.
   *
   * @param allParticipantsAllowed when true, all participants may subscribe and permissions are
   *     ignored
   * @param permissions per-participant permissions applied when allParticipantsAllowed is false
   */
  public void setTrackSubscriptionPermissions(
      boolean allParticipantsAllowed, java.util.List<ParticipantTrackPermission> permissions) {
    if (transport == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    livekit.LivekitRtc.SubscriptionPermission.Builder builder =
        livekit.LivekitRtc.SubscriptionPermission.newBuilder()
            .setAllParticipants(allParticipantsAllowed);
    if (permissions != null) {
      for (ParticipantTrackPermission permission : permissions) {
        builder.addTrackPermissions(permission.toProto());
      }
    }
    transport.sendSubscriptionPermission(builder.build());
  }

  /**
   * @deprecated Use {@link #updateMetadata(String)}.
   */
  @Deprecated
  public void setParticipantMetadata(String metadata) {
    updateMetadata(metadata);
  }

  /**
   * @deprecated Use {@link #updateName(String)}.
   */
  @Deprecated
  public void setParticipantName(String name) {
    updateName(name);
  }

  /**
   * @deprecated Use {@link #updateAttributes(Map)}.
   */
  @Deprecated
  public void setParticipantAttributes(Map<String, String> attributes) {
    updateAttributes(attributes);
  }
}
