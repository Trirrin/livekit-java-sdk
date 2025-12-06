# Data Channels

LiveKit supports real-time data messaging between participants via WebRTC Data Channels. This is useful for chat, game state, file transfer, and other application-level messaging.

## Overview

LiveKit provides two types of data channels:

| Type | Ordered | Reliability | Use Case |
|------|---------|-------------|----------|
| **Reliable** | Yes | Guaranteed delivery | Chat, commands, file transfer |
| **Lossy** | No | Best effort | Game state, cursor position, live updates |

## Sending Data

### Using RtcClient

```java
import io.livekit.sdk.DataPacket;
import io.livekit.sdk.rtc.RtcClient;

RtcClient client = new RtcClient();
client.connect(url, token).join();

// Send reliable data (guaranteed delivery)
byte[] message = "Hello, room!".getBytes();
DataPacket packet = DataPacket.builder(message)
    .kind(DataPacket.Kind.RELIABLE)
    .build();
client.publishData(packet);

// Send lossy data (fast, no retransmission)
byte[] gameState = serializeGameState();
DataPacket lossyPacket = DataPacket.builder(gameState)
    .kind(DataPacket.Kind.LOSSY)
    .build();
client.publishData(lossyPacket);
```

### With Topic

Topics allow you to categorize messages:

```java
DataPacket chatMessage = DataPacket.builder("Hello!".getBytes())
    .kind(DataPacket.Kind.RELIABLE)
    .topic("chat")
    .build();
client.publishData(chatMessage);

DataPacket reaction = DataPacket.builder("👍".getBytes())
    .kind(DataPacket.Kind.RELIABLE)
    .topic("reactions")
    .build();
client.publishData(reaction);
```

### To Specific Participants

```java
// Send to specific participants only
List<String> recipients = Arrays.asList("alice", "bob");
DataPacket privateMessage = DataPacket.builder("Private message".getBytes())
    .kind(DataPacket.Kind.RELIABLE)
    .topic("dm")
    .destinationIdentities(recipients)
    .build();
client.publishData(privateMessage);
```

## Receiving Data

```java
room.addListener(new RoomListener() {
    @Override
    public void onDataReceived(Room room, byte[] data, 
            RemoteParticipant participant, DataPacket.Kind kind, String topic) {
        
        String sender = participant != null ? participant.getIdentity() : "server";
        String message = new String(data);
        
        System.out.println("Data from " + sender + ": " + message);
        System.out.println("Topic: " + topic);
        System.out.println("Kind: " + kind);
        
        // Handle by topic
        if ("chat".equals(topic)) {
            handleChatMessage(sender, message);
        } else if ("game-state".equals(topic)) {
            handleGameState(data);
        }
    }
});
```

## DataPacket API

### Builder

```java
DataPacket.builder(byte[] data)
    .kind(DataPacket.Kind)           // RELIABLE or LOSSY
    .topic(String)                    // Optional topic/category
    .destinationIdentities(List<String>)  // Optional specific recipients
    .build();
```

### Properties

```java
DataPacket packet = ...;
byte[] data = packet.getData();
DataPacket.Kind kind = packet.getKind();
String topic = packet.getTopic();
List<String> recipients = packet.getDestinationIdentities();
```

## Reliable vs Lossy

### Reliable (Default)

- Messages are delivered in order
- Guaranteed delivery with automatic retransmission
- Higher latency under poor network conditions
- Use for: chat, commands, file transfer, critical updates

```java
DataPacket.builder(data)
    .kind(DataPacket.Kind.RELIABLE)
    .build();
```

### Lossy

- Messages may arrive out of order
- No retransmission (lost packets are lost)
- Lower latency, better for real-time updates
- Use for: game state, cursor positions, typing indicators

```java
DataPacket.builder(data)
    .kind(DataPacket.Kind.LOSSY)
    .build();
```

## Common Patterns

### Chat Application

```java
// Send chat message
void sendChat(String message) {
    DataPacket packet = DataPacket.builder(message.getBytes())
        .kind(DataPacket.Kind.RELIABLE)
        .topic("chat")
        .build();
    client.publishData(packet);
}

// Receive chat messages
@Override
public void onDataReceived(Room room, byte[] data, 
        RemoteParticipant participant, DataPacket.Kind kind, String topic) {
    if ("chat".equals(topic)) {
        String sender = participant.getIdentity();
        String message = new String(data);
        displayChatMessage(sender, message);
    }
}
```

### JSON Data

```java
import com.google.gson.Gson;

Gson gson = new Gson();

// Send JSON
MyData data = new MyData("value1", 42);
String json = gson.toJson(data);
DataPacket packet = DataPacket.builder(json.getBytes())
    .kind(DataPacket.Kind.RELIABLE)
    .topic("json-data")
    .build();
client.publishData(packet);

// Receive JSON
@Override
public void onDataReceived(Room room, byte[] data, 
        RemoteParticipant participant, DataPacket.Kind kind, String topic) {
    if ("json-data".equals(topic)) {
        MyData received = gson.fromJson(new String(data), MyData.class);
        process(received);
    }
}
```

### Binary Data

```java
// Send binary data
ByteBuffer buffer = ByteBuffer.allocate(12);
buffer.putInt(1);      // Message type
buffer.putFloat(1.5f); // X position
buffer.putFloat(2.5f); // Y position
byte[] binaryData = buffer.array();

DataPacket packet = DataPacket.builder(binaryData)
    .kind(DataPacket.Kind.LOSSY)
    .topic("position")
    .build();
client.publishData(packet);

// Receive binary data
@Override
public void onDataReceived(Room room, byte[] data, 
        RemoteParticipant participant, DataPacket.Kind kind, String topic) {
    if ("position".equals(topic)) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int type = buffer.getInt();
        float x = buffer.getFloat();
        float y = buffer.getFloat();
        updatePosition(participant.getIdentity(), x, y);
    }
}
```

## E2EE for Data Channels

Data channel messages can be encrypted using E2EE. See [E2EE Guide](e2ee.md) for details.

```java
import io.livekit.sdk.e2ee.E2EEManager;
import io.livekit.sdk.e2ee.E2EEOptions;
import io.livekit.sdk.e2ee.BaseKeyProvider;

// Setup E2EE
BaseKeyProvider keyProvider = new BaseKeyProvider();
keyProvider.setSharedKey(secretKey, 0);

E2EEOptions e2eeOptions = new E2EEOptions(keyProvider);
E2EEManager e2ee = new E2EEManager(e2eeOptions);
e2ee.setDataChannelEncryptionEnabled(true);

// Encrypt before sending
EncryptedPacket encrypted = e2ee.encrypt(plaintext);
// ... send encrypted.toBytes()

// Decrypt on receive
byte[] decrypted = e2ee.decrypt(participantIdentity, encryptedPacket);
```

## Next Steps

- [E2EE](e2ee.md) - Encrypt data channel messages
- [Publishing Media](publishing.md) - Publish audio/video
- [API Reference](api-reference.md) - Complete API documentation
