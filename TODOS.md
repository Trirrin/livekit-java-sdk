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
- `TrackPublication`, `RemoteTrackPublication`, `TrackType`, `TrackSource`
- Participant/track synchronization (join, update, leave, mute)
- Participant metadata/name/attributes updates (`updateMetadata`, `updateName`, `updateAttributes`) with change events
- Subscription controls: `setSubscribed`, `setEnabled`, `setVideoQuality`, `setVideoDimensions`, `setVideoFps`
- Track subscription permissions (`setTrackSubscriptionPermissions`)
- Stream state events (server-paused tracks), subscription failure events

### Signaling
- WebSocket connection with `SignalClient`, **protocol 17** (matches current official SDKs)
- Full `SignalResponse` handling including `RequestResponse`, `SubscribedQualityUpdate`,
  `SubscriptionPermissionUpdate`, `SubscriptionResponse`, `TrackSubscribed`, `RoomMovedResponse`
- State machine: Disconnected → Connecting → Connected → Reconnecting → Failed
- Ping/pong keepalive, automatic reconnection with exponential backoff
- JWT token parsing, ICE server management, ICE restart triggers
- Resume tokens, network change detection, `auto_subscribe` wiring

### RTC (WebRTC via webrtc-java 0.14.0)
- Publisher/Subscriber dual PeerConnection architecture, `addTransceiver`-based publishing
- ICE candidate exchange
- DataChannel (reliable/lossy)
- Media device enumeration (cameras, microphones)
- Local audio/video track creation with audio processing options (AEC/NS/AGC/high-pass)
- **Screen sharing** - screen/window enumeration and publishing (`setScreenShareEnabled`,
  `publishScreenShareTrack`)
- WebRTC stats (`getPublisherStats`, `getSubscriberStats`)
- Preferred video codec selection (`RoomOptions.setPreferredVideoCodec`)

### Client-to-Client APIs
- **RPC** - `performRpc`, `registerRpcMethod` with ack/response tracking, timeouts, and
  official error codes (RPC v1, interoperable with other SDKs)
- **Data Streams** - `sendText`, `streamText`, `sendBytes`, `sendFile`, `streamBytes` plus
  topic-based `registerTextStreamHandler`/`registerByteStreamHandler` with chunked reassembly
- **Chat** - `sendChatMessage`, `editChatMessage`, `onChatMessageReceived`
- **Transcription** - `onTranscriptionReceived` segments
- **SIP DTMF** - `onSipDtmfReceived`

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

## Missing Features (blocked by webrtc-java, require a fork)

The remaining gaps against official client SDKs all need native APIs that
`dev.onvoid.webrtc:webrtc-java` (0.14.0, latest) does not expose:

- [ ] **Simulcast** - `RTCRtpEncodingParameters` lacks the `rid` field required for
  unified-plan simulcast negotiation (small JNI patch: Java field + JNI mapping)
- [ ] **SVC (VP9/AV1)** - `RTCRtpEncodingParameters` lacks `scalabilityMode` (small JNI patch)
- [ ] **Dynacast** - publisher-side layer pausing; depends on simulcast above
- [ ] **Media track E2EE** - `FrameCryptor` only exists in LiveKit's `webrtc-sdk/webrtc` fork of
  libwebrtc; needs forking webrtc-java, building native against the LiveKit fork (or binding
  upstream `FrameTransformer` and implementing the LiveKit frame format in Java), per platform

Recommended approach: one fork of webrtc-java carrying all three patches (rid,
scalabilityMode, FrameCryptor bindings), since the native build setup dominates the cost.

## Missing Features (not blocked)

- [ ] **Integration tests** - End-to-end tests with real LiveKit server
- [ ] **Interoperability tests** - Cross-SDK testing (Web/Android/Go)
- [ ] API documentation / user guide

---

## Test Summary

| Module | Tests | Status |
|--------|-------|--------|
| core | 81 | ✅ |
| signaling | 26 | ✅ |
| rtc | 15 | ✅ |
| **Total** | **122** | **All passing** |
