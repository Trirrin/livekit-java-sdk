# API Reference

Complete API reference for the LiveKit Java SDK.

## Modules

| Module | Description |
|--------|-------------|
| `protocol` | Protobuf definitions and generated classes |
| `core` | Room, Participant, Track, E2EE APIs |
| `signaling` | WebSocket signaling client |
| `rtc` | WebRTC integration with webrtc-java |

---

## Core Module (`io.livekit.sdk`)

### Room

The main entry point for interacting with a LiveKit room.

```java
public class Room {
    // Constructor
    Room()
    Room(RoomOptions options)
    
    // Connection
    void connect(String url, String token)
    void disconnect()
    
    // Properties
    String getSid()
    String getName()
    String getMetadata()
    ConnectionState getState()
    RoomOptions getOptions()
    
    // Participants
    LocalParticipant getLocalParticipant()
    Map<String, RemoteParticipant> getRemoteParticipants()
    RemoteParticipant getRemoteParticipant(String identity)
    
    // Data
    void publishData(DataPacket packet)
    
    // Listeners
    void addListener(RoomListener listener)
    void removeListener(RoomListener listener)
}
```

### RoomOptions

Configuration options for room connection.

```java
public class RoomOptions {
    // Auto-subscribe to tracks (default: true)
    boolean isAutoSubscribe()
    RoomOptions setAutoSubscribe(boolean autoSubscribe)
    
    // Adaptive streaming (default: false)
    boolean isAdaptiveStream()
    RoomOptions setAdaptiveStream(boolean adaptiveStream)
    
    // Dynamic broadcast (default: false)
    boolean isDynacast()
    RoomOptions setDynacast(boolean dynacast)
    
    // Reconnection settings
    int getReconnectAttempts()          // default: 5
    RoomOptions setReconnectAttempts(int attempts)
    long getReconnectDelayMs()          // default: 1000
    RoomOptions setReconnectDelayMs(long delayMs)
    
    // E2EE
    boolean isE2eeEnabled()
    RoomOptions setE2eeEnabled(boolean enabled)
    E2EEOptions getE2eeOptions()
    RoomOptions setE2eeOptions(E2EEOptions options)
}
```

### RoomListener

Interface for receiving room events.

```java
public interface RoomListener {
    // Connection events
    void onConnected(Room room)
    void onDisconnected(Room room, DisconnectReason reason)
    void onReconnecting(Room room)
    void onReconnected(Room room)
    
    // Participant events
    void onParticipantConnected(Room room, RemoteParticipant participant)
    void onParticipantDisconnected(Room room, RemoteParticipant participant)
    void onParticipantMetadataChanged(Room room, Participant participant, String prevMetadata)
    
    // Track events
    void onTrackPublished(Room room, TrackPublication publication, Participant participant)
    void onTrackUnpublished(Room room, TrackPublication publication, Participant participant)
    void onTrackSubscribed(Room room, Track track, TrackPublication publication, RemoteParticipant participant)
    void onTrackUnsubscribed(Room room, Track track, TrackPublication publication, RemoteParticipant participant)
    void onTrackMuted(Room room, TrackPublication publication, Participant participant)
    void onTrackUnmuted(Room room, TrackPublication publication, Participant participant)
    
    // Data events
    void onDataReceived(Room room, byte[] data, RemoteParticipant participant, DataPacket.Kind kind, String topic)
    
    // Other events
    void onActiveSpeakersChanged(Room room, List<Participant> speakers)
    void onConnectionQualityChanged(Room room, Participant participant, ConnectionQuality quality)
    void onRoomMetadataChanged(Room room, String metadata)
}
```

### Participant

Base class for all participants.

```java
public abstract class Participant {
    String getSid()
    String getIdentity()
    String getName()
    String getMetadata()
    Map<String, String> getAttributes()
    ConnectionQuality getConnectionQuality()
    boolean isSpeaking()
    long getAudioLevel()
    
    // Tracks
    Map<String, TrackPublication> getTrackPublications()
    TrackPublication getTrackPublication(String sid)
    TrackPublication getTrackPublicationBySource(TrackSource source)
}
```

### LocalParticipant

The local user in the room.

```java
public class LocalParticipant extends Participant {
    // Publishing
    void publishTrack(Track track)
    void unpublishTrack(Track track)
    
    // Media controls
    void setMicrophoneEnabled(boolean enabled)
    void setCameraEnabled(boolean enabled)
    void setScreenShareEnabled(boolean enabled)
    
    // Metadata
    void setParticipantMetadata(String metadata)
    void setParticipantName(String name)
    void setParticipantAttributes(Map<String, String> attributes)
}
```

### RemoteParticipant

A remote participant in the room.

```java
public class RemoteParticipant extends Participant {
    // Inherits all Participant methods
}
```

### Track

Base class for media tracks.

```java
public abstract class Track {
    String getSid()
    String getName()
    TrackType getType()
    TrackSource getSource()
    boolean isMuted()
}
```

### TrackPublication

Represents a published track.

```java
public class TrackPublication {
    String getSid()
    String getName()
    TrackType getType()
    TrackSource getSource()
    Track getTrack()
    boolean isMuted()
    boolean isSubscribed()
    String getMimeType()
}
```

### DataPacket

Represents a data packet for messaging.

```java
public class DataPacket {
    enum Kind { RELIABLE, LOSSY }
    
    // Builder
    static Builder builder(byte[] data)
    
    // Properties
    byte[] getData()
    Kind getKind()
    String getTopic()
    List<String> getDestinationIdentities()
    
    // Builder class
    class Builder {
        Builder kind(Kind kind)
        Builder topic(String topic)
        Builder destinationIdentities(List<String> identities)
        DataPacket build()
    }
}
```

### Enums

```java
public enum ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

public enum ConnectionQuality {
    UNKNOWN, EXCELLENT, GOOD, POOR, LOST
}

public enum TrackType {
    AUDIO, VIDEO, DATA
}

public enum TrackSource {
    UNKNOWN, CAMERA, MICROPHONE, SCREEN_SHARE, SCREEN_SHARE_AUDIO
}

public enum DisconnectReason {
    UNKNOWN, CLIENT_INITIATED, DUPLICATE_IDENTITY, SERVER_SHUTDOWN,
    PARTICIPANT_REMOVED, ROOM_DELETED, STATE_MISMATCH, JOIN_FAILURE
}
```

---

## Signaling Module (`io.livekit.sdk.signaling`)

### LiveKitClient

Main client for signaling-only connections.

```java
public class LiveKitClient {
    LiveKitClient()
    LiveKitClient(RoomOptions options)
    
    CompletableFuture<Room> connect(String url, String token)
    void disconnect()
    void shutdown()
    
    Room getRoom()
    SignalClient getSignalClient()
}
```

### SignalClient

Low-level WebSocket signaling client.

```java
public class SignalClient {
    void connect(String url, String token)
    void disconnect()
    void shutdown()
    
    SignalState getState()
    void addListener(SignalListener listener)
    void removeListener(SignalListener listener)
    
    // Signaling operations
    void sendOffer(SessionDescription offer)
    void sendAnswer(SessionDescription answer)
    void sendTrickle(TrickleRequest trickle)
    void sendLeave()
    void sendPing()
}
```

### SignalState

```java
public enum SignalState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED
}
```

---

## RTC Module (`io.livekit.sdk.rtc`)

### RtcClient

Full-featured client with WebRTC support.

```java
public class RtcClient {
    RtcClient()
    RtcClient(RoomOptions options)
    
    // Connection
    CompletableFuture<Room> connect(String url, String token)
    void disconnect()
    void shutdown()
    
    // Components
    Room getRoom()
    SignalClient getSignalClient()
    PeerConnectionEngine getRtcEngine()
    
    // Publishing
    void publishAudioTrack(LocalAudioTrack track)
    void publishVideoTrack(LocalVideoTrack track)
    void unpublishTrack(String trackId)
    boolean publishData(DataPacket packet)
    
    // Device enumeration
    List<MediaDeviceInfo> getAudioInputDevices()
    List<MediaDeviceInfo> getAudioOutputDevices()
    List<MediaDeviceInfo> getVideoInputDevices()
    
    // Track creation
    LocalAudioTrack createAudioTrack()
    LocalAudioTrack createAudioTrack(String deviceId, String name)
    LocalVideoTrack createVideoTrack()
    LocalVideoTrack createVideoTrack(String deviceId, String name, int width, int height, int frameRate)
}
```

### LocalAudioTrack

```java
public class LocalAudioTrack extends Track {
    LocalAudioTrack(String sid, String name, AudioTrack nativeTrack)
    AudioTrack getNativeTrack()
}
```

### LocalVideoTrack

```java
public class LocalVideoTrack extends Track {
    LocalVideoTrack(String sid, String name, VideoTrack nativeTrack)
    VideoTrack getNativeTrack()
}
```

### RemoteAudioTrack

```java
public class RemoteAudioTrack extends Track {
    RemoteAudioTrack(String sid, String name, AudioTrack nativeTrack)
    AudioTrack getNativeTrack()
}
```

### RemoteVideoTrack

```java
public class RemoteVideoTrack extends Track {
    RemoteVideoTrack(String sid, String name, VideoTrack nativeTrack)
    VideoTrack getNativeTrack()
}
```

### MediaDeviceInfo

```java
public class MediaDeviceInfo {
    enum Kind { AUDIO_INPUT, AUDIO_OUTPUT, VIDEO_INPUT }
    
    String getDeviceId()
    String getLabel()
    Kind getKind()
}
```

### DataChannelManager

```java
public class DataChannelManager {
    static final String RELIABLE_CHANNEL_LABEL = "_reliable"
    static final String LOSSY_CHANNEL_LABEL = "_lossy"
    
    void createDataChannels(RTCPeerConnection peerConnection)
    void onDataChannel(RTCDataChannel channel)
    boolean send(byte[] data, boolean reliable)
    boolean isReliableOpen()
    boolean isLossyOpen()
    void close()
    
    void setListener(DataChannelListener listener)
    
    interface DataChannelListener {
        void onDataChannelOpen(boolean reliable)
        void onDataReceived(byte[] data, boolean reliable)
    }
}
```

---

## E2EE Module (`io.livekit.sdk.e2ee`)

### E2EEManager

Central E2EE coordinator.

```java
public class E2EEManager {
    E2EEManager(E2EEOptions options)
    
    // Configuration
    E2EEOptions getOptions()
    KeyProvider getKeyProvider()
    boolean isEnabled()
    void setEnabled(boolean enabled)
    boolean isDataChannelEncryptionEnabled()
    void setDataChannelEncryptionEnabled(boolean enabled)
    void setLocalIdentity(String identity)
    
    // Key operations
    void setSharedKey(byte[] key, int keyIndex)
    void setKey(byte[] key, int keyIndex, String participantIdentity)
    KeyInfo rotateKey()
    byte[] ratchetSharedKey()
    
    // Encryption/decryption
    EncryptedPacket encrypt(byte[] payload)
    byte[] decrypt(String participantIdentity, EncryptedPacket packet)
    
    // Listeners
    void addListener(E2EEListener listener)
    void removeListener(E2EEListener listener)
    
    // Cleanup
    void cleanup()
}
```

### E2EEOptions

```java
public class E2EEOptions {
    E2EEOptions()
    E2EEOptions(KeyProvider keyProvider)
    E2EEOptions(EncryptionType encryptionType, KeyProvider keyProvider)
    
    EncryptionType getEncryptionType()
    KeyProvider getKeyProvider()
    
    static Builder builder()
    
    class Builder {
        Builder encryptionType(EncryptionType type)
        Builder keyProvider(KeyProvider provider)
        Builder ratchetSalt(String salt)
        Builder ratchetWindowSize(int size)
        Builder sharedKeyMode(boolean sharedKey)
        E2EEOptions build()
    }
}
```

### KeyProvider

Interface for key management.

```java
public interface KeyProvider {
    KeyInfo getKey(String participantIdentity)
    KeyInfo getKey(String participantIdentity, int keyIndex)
    void setKey(byte[] key, int keyIndex, String participantIdentity)
    KeyInfo rotateKey()
    
    KeyInfo getSharedKey()
    void setSharedKey(byte[] key, int keyIndex)
    byte[] ratchetSharedKey(int keyIndex)
    byte[] ratchetKey(String participantIdentity, int keyIndex)
    
    byte[] exportKey(String participantIdentity)
    byte[] exportSharedKey(int keyIndex)
    int getLatestKeyIndex(String participantIdentity)
    
    void clearKeys(String participantIdentity)
    void clearAllKeys()
    boolean isSharedKeyMode()
}
```

### BaseKeyProvider

Default KeyProvider implementation.

```java
public class BaseKeyProvider implements KeyProvider {
    static final int DEFAULT_KEY_SIZE = 32  // 256 bits
    static final String DEFAULT_RATCHET_SALT = "LKFrameEncryptionKey"
    static final int DEFAULT_RATCHET_WINDOW_SIZE = 16
    
    BaseKeyProvider(boolean sharedKeyMode)
    BaseKeyProvider(boolean sharedKeyMode, String ratchetSalt, int ratchetWindowSize)
    
    void setLocalIdentity(String identity)
    byte[] generateKey()
    int getRatchetWindowSize()
    
    // Implements all KeyProvider methods
}
```

### KeyInfo

```java
public class KeyInfo {
    KeyInfo(byte[] key, int keyIndex, String participantIdentity)
    
    byte[] getKey()
    int getKeyIndex()
    String getParticipantIdentity()
}
```

### EncryptedPacket

```java
public class EncryptedPacket {
    EncryptedPacket(byte[] payload, byte[] iv, int keyIndex)
    
    byte[] getPayload()
    byte[] getIv()
    int getKeyIndex()
    
    byte[] toBytes()
    static EncryptedPacket fromBytes(byte[] bytes)
}
```

### E2EEListener

```java
public interface E2EEListener {
    void onE2EEStateChanged(boolean enabled)
    void onKeySet(int keyIndex)
    void onKeyRotated(KeyInfo newKey)
    void onKeyRatcheted(String participantIdentity, int keyIndex)
    void onEncryptionError(String participantIdentity, E2EEException error)
}
```

### Enums

```java
public enum EncryptionType {
    NONE, GCM, CUSTOM
}

public enum E2EEState {
    NEW, OK, ENCRYPTION_FAILED, DECRYPTION_FAILED, MISSING_KEY, KEY_RATCHETED, INTERNAL_ERROR
}
```

---

## Protocol Module (`livekit.*`)

Generated protobuf classes for LiveKit protocol. Key packages:

- `livekit.LivekitModels` - Core data models (Room, Participant, Track, DataPacket)
- `livekit.LivekitRtc` - RTC signaling messages (JoinResponse, SessionDescription, TrickleRequest)

See [LiveKit Protocol](https://github.com/livekit/protocol) for protobuf definitions.
