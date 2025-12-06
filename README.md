# LiveKit Java SDK

English | [简体中文](README_CN.md)

Java client SDK for [LiveKit](https://livekit.io) real-time communication platform.

## Features

- **Room Management** - Join/leave rooms, handle participants and tracks
- **WebRTC Integration** - Audio/video publishing and subscribing via [webrtc-java](https://github.com/niclasvaneyk/webrtc-java)
- **Data Channels** - Reliable and lossy data messaging
- **E2EE** - End-to-end encryption for data channels (AES-GCM with key ratcheting)
- **Auto Reconnection** - Automatic reconnection with exponential backoff
- **Network Monitoring** - Resume tokens and network change detection

## Requirements

- Java 21+
- Gradle 9.x (included via wrapper)

## Installation

[![](https://jitpack.io/v/Trirrin/livekit-java-sdk.svg)](https://jitpack.io/#Trirrin/livekit-java-sdk)

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Full SDK with WebRTC support
    implementation("com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0")
    
    // Or signaling only (no audio/video)
    // implementation("com.github.Trirrin.livekit-java-sdk:signaling:v0.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Trirrin.livekit-java-sdk</groupId>
        <artifactId>rtc</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

## Quick Start

```java
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.signaling.LiveKitClient;

public class Example {
    public static void main(String[] args) {
        RoomOptions options = new RoomOptions();
        LiveKitClient client = new LiveKitClient(options);
        Room room = client.getRoom();

        room.addListener(new RoomListener() {
            @Override
            public void onConnected(Room r) {
                System.out.println("Connected to room: " + r.getName());
            }

            @Override
            public void onParticipantConnected(Room r, RemoteParticipant participant) {
                System.out.println("Participant joined: " + participant.getIdentity());
            }
            
            // ... other callbacks
        });

        client.connect("wss://your-server.livekit.cloud", "your-access-token");
    }
}
```

## Examples

The `examples` module includes working examples:

| Example | Description |
|---------|-------------|
| `BasicRoomExample` | Join room and handle events |
| `PublishExample` | Publish audio/video tracks |
| `SubscribeExample` | Subscribe to remote tracks |
| `DataChannelExample` | Interactive data messaging |

Run examples:

```bash
export LIVEKIT_URL=wss://your-server.livekit.cloud
export LIVEKIT_TOKEN=your-access-token
./gradlew :examples:run
```

## Documentation

- [Getting Started](docs/getting-started.md) - Installation and basic usage
- [Publishing Media](docs/publishing.md) - Audio/video publishing guide
- [Data Channels](docs/data-channels.md) - Real-time messaging
- [E2EE Guide](docs/e2ee.md) - End-to-end encryption
- [API Reference](docs/api-reference.md) - Complete API documentation

## Project Structure

```
livekit-java-sdk/
├── protocol/    # Protobuf definitions and generated classes
├── core/        # Room, Participant, Track, E2EE APIs
├── signaling/   # WebSocket signaling client
├── rtc/         # WebRTC integration
├── examples/    # Usage examples
└── docs/        # Documentation
```

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Format code
./gradlew spotlessApply

# Publish to local repository
./gradlew publishToMavenLocal
```

## E2EE (End-to-End Encryption)

Data channel encryption is supported via `E2EEManager`:

```java
import io.livekit.sdk.e2ee.E2EEManager;
import io.livekit.sdk.e2ee.BaseKeyProvider;

BaseKeyProvider keyProvider = new BaseKeyProvider();
keyProvider.setSharedKey(secretKeyBytes);

E2EEManager e2ee = new E2EEManager(keyProvider);
byte[] encrypted = e2ee.encryptData(plaintext, participantId);
byte[] decrypted = e2ee.decryptData(encrypted, participantId);
```

**Note:** Media track E2EE requires `FrameCryptor` API which is not yet exposed by webrtc-java.

## Known Limitations

- Screen sharing not yet exposed (webrtc-java supports it)
- Simulcast/SVC layer selection not implemented
- Media track E2EE blocked by webrtc-java library limitation

## License

Apache License 2.0

## Links

- [LiveKit Documentation](https://docs.livekit.io)
- [LiveKit Server](https://github.com/livekit/livekit)
- [webrtc-java](https://github.com/niclasvaneyk/webrtc-java)
