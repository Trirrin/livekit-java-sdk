# LiveKit Java SDK - Status

## Implemented Features

### Core Infrastructure
- **Protocol Module** - LiveKit protobuf messages (models, rtc, room, egress, ingress, sip)
- **Build System** - Gradle 9.x, Java 21, JUnit 5, Spotless formatting, GitHub Actions CI
- **Maven Publishing** - Source/Javadoc JARs, POM metadata for Maven Central

### Room & Participants
- `Room`, `RoomOptions`, `RoomListener`, `RoomEvent`
- `Participant`, `LocalParticipant`, `RemoteParticipant`
- `Track`, `AudioTrack`, `VideoTrack`, `DataTrack`
- `TrackPublication`, `TrackType`, `TrackSource`
- Participant/track synchronization (join, update, leave, mute)

### Signaling
- WebSocket connection with `SignalClient`
- State machine: Disconnected → Connecting → Connected → Reconnecting → Failed
- Ping/pong keepalive, automatic reconnection with exponential backoff
- JWT token parsing, ICE server management, ICE restart triggers
- Resume tokens, network change detection

### RTC (WebRTC via webrtc-java 0.14.0)
- Publisher/Subscriber dual PeerConnection architecture
- ICE candidate exchange
- DataChannel (reliable/lossy)
- Media device enumeration (cameras, microphones)
- Local audio/video track creation

### E2EE (End-to-End Encryption)
- **DataChannel E2EE** - AES-GCM encryption for data messages
- Key management with `KeyProvider`, `BaseKeyProvider`
- Key ratcheting (HMAC-SHA256 derivation)
- Shared key and per-participant key modes

### Examples
- `BasicRoomExample` - Join room and handle events
- `PublishExample` - Publish audio/video tracks
- `SubscribeExample` - Subscribe to remote tracks
- `DataChannelExample` - Interactive data messaging

---

## Missing Features

### Media
- [ ] **Screen sharing** - webrtc-java supports `ScreenCapturer`/`WindowCapturer`, not yet exposed in SDK
- [ ] **Simulcast/SVC** - Layer selection for adaptive streaming
- [ ] **Audio processing** - Noise suppression, echo cancellation configuration

### E2EE
- [ ] **Media track E2EE** - Requires `FrameCryptor` API not exposed by webrtc-java (blocked by library limitation)

### Testing
- [ ] **Integration tests** - End-to-end tests with real LiveKit server
- [ ] **Interoperability tests** - Cross-SDK testing (Web/Android/Go)

### Documentation
- [ ] API documentation
- [ ] User guide

---

## Test Summary

| Module | Tests | Status |
|--------|-------|--------|
| core | 36 | ✅ |
| signaling | 19 | ✅ |
| rtc | 15 | ✅ |
| **Total** | **70** | **All passing** |
