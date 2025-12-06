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

### Pending

7. RTC/media stack selection
   - [ ] Choose Java-accessible WebRTC implementation (libwebrtc JNI build vs alternative)
   - [ ] Validate media/datachannel interoperability with official SDKs
   - [ ] Plan audio processing (AEC/NS/AGC) and video encoding params

8. Resilience & security
   - [ ] Implement resume tokens and network change handling
   - [ ] Support SRTP; design E2EE hooks and key rotation interfaces

9. Testing & validation
   - [ ] Add interoperability tests with Web/Android/Go SDKs
   - [ ] Add CI targets: lint/format (spotless), protoc generation check
   - [ ] Define sample apps for join/publish/subscribe

10. Packaging
    - [ ] Gradle/Maven artifacts
    - [ ] Documentation and examples

---

## Progress Log

### 2024-12-06: Task 6 (Room Integration) Completed
- Created `ProtoConverter` for mapping protobuf messages to SDK types
- Enhanced `Room` with signal handling methods for join, participants, tracks, room updates
- Created `RoomSignalHandler` to bridge SignalClient events to Room
- Created `LiveKitClient` as main entry point coordinating Room + SignalClient
- Added comprehensive unit tests for `ProtoConverter` and `RoomSignalHandler`
- All tests passing
