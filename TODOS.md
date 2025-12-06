## TODOs

### Completed

1. **Protocol module** [DONE]
   - Imported livekit/protocol protobufs (excluding server-side RPC)
   - Generated Java classes for: models, rtc, room, egress, ingress, sip, etc.
   - Configured Gradle protobuf plugin with proper exclusions

2. **Core module** [DONE]
   - Implemented core types aligned with Web/Android SDK naming:
     - `Room`, `RoomOptions`, `RoomListener`, `RoomEvent`
     - `Participant`, `LocalParticipant`, `RemoteParticipant`
     - `Track`, `AudioTrack`, `VideoTrack`, `DataTrack`
     - `TrackPublication`, `TrackType`, `TrackSource`
     - `DataPacket`, `ConnectionState`, `ConnectionQuality`, `DisconnectReason`
   - Event system with listener pattern

3. **Signaling module** [DONE]
   - `SignalClient` with WebSocket connection (Java-WebSocket library)
   - State machine: Disconnected → Connecting → Connected → Reconnecting → Failed
   - Ping/pong keepalive with timeout detection
   - Automatic reconnection with exponential backoff
   - `TokenParser` for JWT claim extraction (no signature verification)
   - Full SignalRequest/SignalResponse message handling

4. **Build infrastructure** [DONE]
   - Gradle 9.x multi-module setup
   - Java 21 target
   - JUnit 5 tests passing
   - Modules: protocol, core, signaling

5. Protocol & signaling [DONE]
   - [x] Import livekit/protocol protobufs and generate Java classes.
   - [x] Implement WebSocket signaling: join/offer/answer/track updates/ping-pong/room state, token parsing.
   - [x] Design state machine: Disconnected → Connecting → Connected → Reconnecting → Failing with resume/backoff.
   - [x] Handle ICE restart triggers and track publication/subscription sync after resume.
     - Added `ReconnectReason` enum for tracking reconnection causes
     - Implemented `SyncStateBuilder` for constructing client state sync messages
     - Enhanced `SignalClient` with ICE server management and restart triggers
     - Added `onIceServersUpdated` and `onIceRestartRequired` listener callbacks

6. Room integration [DONE]
   - [x] Connect Room to SignalClient via `LiveKitClient` and `RoomSignalHandler`
   - [x] Map protobuf messages to SDK types via `ProtoConverter`
   - [x] Implement participant/track synchronization (join, update, leave, track pub/unpub, mute)

7. RTC module [DONE]
   - [x] Chose `dev.onvoid.webrtc:webrtc-java:0.14.0` (libwebrtc m140 JNI bindings)
   - [x] Created `rtc` module with PeerConnectionEngine
   - [x] Implemented RtcEngine interface abstracting WebRTC operations
   - [x] Created RtcClient coordinating Room + SignalClient + RtcEngine
   - [x] Publisher/Subscriber dual PeerConnection architecture
   - [x] ICE candidate exchange with JSON parsing

8. RTC integration completion [DONE]
   - [x] Wire remote track reception to Room subscriptions
   - [x] Implement DataChannel for reliable/unreliable data
   - [x] Add media device enumeration and track creation helpers

9. Resilience & security [DONE]
   - [x] Implement resume tokens and network change handling
   - [x] Design E2EE hooks and key rotation interfaces

### Pending

10. Testing & validation
    - [ ] Add interoperability tests with Web/Android/Go SDKs (requires live server)

11. Documentation
    - [ ] Add comprehensive API documentation
    - [ ] Create user guide

### Completed (continued)

10. Testing & validation [PARTIAL]
    - [x] Add CI targets: lint/format (spotless), protoc generation check
    - [x] Define sample apps for join/publish/subscribe (examples module)
    - GitHub Actions CI workflow at `.github/workflows/ci.yml`

11. Packaging [DONE]
    - [x] Gradle/Maven artifacts configured
    - [x] Source and Javadoc JARs generation
    - [x] POM metadata for Maven Central publishing
    - [x] Local Maven repository publishing support

---

## Progress Log

### 2024-12-06: Task 6 (Room Integration) Completed
- Created `ProtoConverter` for mapping protobuf messages to SDK types
- Enhanced `Room` with signal handling methods for join, participants, tracks, room updates
- Created `RoomSignalHandler` to bridge SignalClient events to Room
- Created `LiveKitClient` as main entry point coordinating Room + SignalClient
- Added comprehensive unit tests for `ProtoConverter` and `RoomSignalHandler`
- All tests passing

### 2025-12-06: Task 9 (Resilience & Security) Completed
- Created `ResumeTokenManager` for storing and refreshing access tokens during reconnection
- Implemented `NetworkMonitor` interface and `DefaultNetworkMonitor` for detecting network changes
- Integrated network monitoring into `SignalClient` for automatic reconnection on network recovery
- Designed E2EE framework in `core/src/main/java/io/livekit/sdk/e2ee/`:
  - `EncryptionType` enum (NONE, GCM, CUSTOM)
  - `KeyInfo` for key metadata
  - `KeyProvider` interface for key management
  - `BaseKeyProvider` implementation with shared/per-participant key modes
  - `FrameCryptor` interface for frame encryption/decryption
  - `E2EEManager` for coordinating E2EE state
  - `E2EEListener` for E2EE events
  - `E2EEOptions` for configuration
  - `E2EEException` for error handling
- Updated `RoomOptions` to support E2EE configuration
- All tests passing

### 2024-12-06: Task 7 (RTC Module) Completed
- Selected `dev.onvoid.webrtc:webrtc-java:0.14.0` (libwebrtc m140, same approach as Android SDK)
- Created `rtc` module with WebRTC integration
- Implemented `RtcEngine` interface and `PeerConnectionEngine` implementation
- Dual PeerConnection architecture: Publisher (send) + Subscriber (receive)
- Created `RtcClient` coordinating Room + SignalClient + RtcEngine
- ICE candidate JSON parsing with `IceCandidateParser`
- Unit tests for ICE parsing and configuration

### 2024-12-06: Task 8 (RTC Integration Completion) Completed
- Wired remote track reception to Room subscriptions via `mid_to_track_id` mapping
- Created `RemoteAudioTrack` and `RemoteVideoTrack` wrappers with native track access
- Added `TrackSubscriptionHandler` interface for Room to receive track events
- Implemented `DataChannelManager` for reliable/lossy data transport
- Created dual data channels on publisher connection (_reliable and _lossy)
- Added protobuf DataPacket serialization/deserialization in RtcClient
- Created `MediaDevicesHelper` for device enumeration and track creation
- Added `MediaDeviceInfo` for device metadata
- Exposed convenience methods in RtcClient for audio/video device listing and track creation
- All tests passing

### 2025-12-06: Task 10 & 11 (CI, Examples, Packaging) Completed
- Added Spotless plugin for code formatting (Google Java Format 1.22.0)
- Created GitHub Actions CI workflow (`.github/workflows/ci.yml`):
  - Spotless check for code formatting
  - Protobuf generation verification
  - Build and test execution
  - Test report artifact upload on failure
- Created `examples` module with 4 sample applications:
  - `BasicRoomExample`: Join room and handle all events
  - `PublishExample`: Publish audio/video tracks
  - `SubscribeExample`: Subscribe to remote tracks
  - `DataChannelExample`: Interactive data messaging
- Configured Maven publishing for all library modules:
  - Source and Javadoc JAR generation
  - POM metadata for Maven Central compatibility
  - Local Maven repository publishing
  - Signing configuration (optional)
- All tests passing, spotless check passing
