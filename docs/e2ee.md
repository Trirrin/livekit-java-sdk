# End-to-End Encryption (E2EE)

This guide covers the end-to-end encryption capabilities of the LiveKit Java SDK.

## Overview

The SDK supports E2EE for **data channel messages** using AES-256-GCM encryption. E2EE ensures that data is encrypted on the sender's device and only decrypted on authorized recipients' devices - the LiveKit server cannot read the content.

> **Note**: Media track E2EE (audio/video) requires WebRTC's `FrameCryptor` API which is not yet exposed by webrtc-java. This SDK currently supports data channel E2EE only.

## Key Concepts

| Component | Description |
|-----------|-------------|
| **E2EEManager** | Central coordinator for E2EE operations |
| **KeyProvider** | Interface for key storage and derivation |
| **BaseKeyProvider** | Default implementation with in-memory storage |
| **DataPacketCryptor** | AES-GCM encryption/decryption engine |

## Quick Start

### Setup E2EE

```java
import io.livekit.sdk.e2ee.*;

// Create key provider in shared key mode
BaseKeyProvider keyProvider = new BaseKeyProvider(true);

// Generate or use a shared secret key (32 bytes for AES-256)
byte[] sharedSecret = keyProvider.generateKey();
keyProvider.setSharedKey(sharedSecret, 0);

// Create E2EE options
E2EEOptions e2eeOptions = new E2EEOptions(keyProvider);

// Create E2EE manager
E2EEManager e2ee = new E2EEManager(e2eeOptions);
e2ee.setDataChannelEncryptionEnabled(true);
e2ee.setLocalIdentity("alice");
```

### Encrypt Data

```java
// Encrypt a message
byte[] plaintext = "Secret message".getBytes();
EncryptedPacket encrypted = e2ee.encrypt(plaintext);

if (encrypted != null) {
    // Send encrypted.toBytes() via data channel
    byte[] payload = encrypted.toBytes();
    DataPacket packet = DataPacket.builder(payload)
        .kind(DataPacket.Kind.RELIABLE)
        .topic("encrypted")
        .build();
    client.publishData(packet);
}
```

### Decrypt Data

```java
@Override
public void onDataReceived(Room room, byte[] data, 
        RemoteParticipant participant, DataPacket.Kind kind, String topic) {
    
    if ("encrypted".equals(topic)) {
        // Parse encrypted packet
        EncryptedPacket encrypted = EncryptedPacket.fromBytes(data);
        
        // Decrypt
        byte[] plaintext = e2ee.decrypt(participant.getIdentity(), encrypted);
        if (plaintext != null) {
            String message = new String(plaintext);
            System.out.println("Decrypted: " + message);
        } else {
            System.err.println("Decryption failed");
        }
    }
}
```

## E2EEOptions

### Using Builder

```java
E2EEOptions options = E2EEOptions.builder()
    .encryptionType(EncryptionType.GCM)     // AES-GCM (default)
    .sharedKeyMode(true)                     // Use shared key for all participants
    .ratchetSalt("MyCustomSalt")             // Custom salt for key derivation
    .ratchetWindowSize(16)                   // Window size for key ratcheting
    .build();
```

### Using Constructor

```java
// With default key provider
E2EEOptions options = new E2EEOptions();

// With custom key provider
BaseKeyProvider customProvider = new BaseKeyProvider(true);
E2EEOptions options = new E2EEOptions(customProvider);
```

## Key Management

### Shared Key Mode

All participants use the same key. Simpler setup, but if one participant's key is compromised, all communication is compromised.

```java
// Enable shared key mode
BaseKeyProvider keyProvider = new BaseKeyProvider(true);

// Set the shared key
byte[] sharedKey = generateOrReceiveKey();
keyProvider.setSharedKey(sharedKey, 0);

// All participants must have the same key
```

### Per-Participant Key Mode

Each participant has their own key. More secure but requires key exchange.

```java
// Disable shared key mode
BaseKeyProvider keyProvider = new BaseKeyProvider(false);

// Set local key
byte[] myKey = keyProvider.generateKey();
keyProvider.setKey(myKey, 0, "alice");

// Set remote participant's key (received via secure channel)
byte[] bobKey = receiveKeyFromBob();
keyProvider.setKey(bobKey, 0, "bob");
```

### Key Rotation

Rotate keys periodically for forward secrecy:

```java
// Rotate to a new key (uses key ratcheting)
KeyInfo newKey = e2ee.rotateKey();
System.out.println("New key index: " + newKey.getKeyIndex());

// Or ratchet shared key manually
byte[] newSharedKey = e2ee.ratchetSharedKey();
```

### Key Ratcheting

Key ratcheting derives new keys from existing keys using HMAC-SHA256:

```java
// Ratchet shared key
byte[] derivedKey = keyProvider.ratchetSharedKey(currentKeyIndex);

// Ratchet participant-specific key
byte[] derivedKey = keyProvider.ratchetKey("alice", currentKeyIndex);
```

## E2EEManager API

### Configuration

```java
E2EEManager e2ee = new E2EEManager(options);

// Set local participant identity (for key lookup)
e2ee.setLocalIdentity("alice");

// Enable/disable E2EE globally
e2ee.setEnabled(true);

// Enable/disable data channel encryption specifically
e2ee.setDataChannelEncryptionEnabled(true);

// Check status
boolean enabled = e2ee.isEnabled();
boolean dataEnabled = e2ee.isDataChannelEncryptionEnabled();
```

### Key Operations

```java
// Set shared key
e2ee.setSharedKey(keyBytes, keyIndex);

// Set participant-specific key
e2ee.setKey(keyBytes, keyIndex, "bob");

// Rotate key
KeyInfo newKey = e2ee.rotateKey();

// Ratchet shared key
byte[] newKeyBytes = e2ee.ratchetSharedKey();
```

### Encryption/Decryption

```java
// Encrypt
EncryptedPacket encrypted = e2ee.encrypt(plaintext);

// Decrypt
byte[] decrypted = e2ee.decrypt(participantIdentity, encryptedPacket);
```

## Event Listeners

```java
e2ee.addListener(new E2EEListener() {
    @Override
    public void onE2EEStateChanged(boolean enabled) {
        System.out.println("E2EE " + (enabled ? "enabled" : "disabled"));
    }
    
    @Override
    public void onKeySet(int keyIndex) {
        System.out.println("Key set at index: " + keyIndex);
    }
    
    @Override
    public void onKeyRotated(KeyInfo newKey) {
        System.out.println("Key rotated to index: " + newKey.getKeyIndex());
    }
    
    @Override
    public void onKeyRatcheted(String participantIdentity, int keyIndex) {
        System.out.println("Key ratcheted for " + participantIdentity);
    }
    
    @Override
    public void onEncryptionError(String participantIdentity, E2EEException error) {
        System.err.println("E2EE error for " + participantIdentity + ": " + error.getMessage());
    }
});
```

## KeyProvider Interface

Implement custom key storage (e.g., secure enclave, HSM):

```java
public interface KeyProvider {
    // Get current key for participant
    KeyInfo getKey(String participantIdentity);
    KeyInfo getKey(String participantIdentity, int keyIndex);
    
    // Set key for participant
    void setKey(byte[] key, int keyIndex, String participantIdentity);
    
    // Key rotation
    KeyInfo rotateKey();
    
    // Shared key operations
    KeyInfo getSharedKey();
    void setSharedKey(byte[] key, int keyIndex);
    
    // Key ratcheting
    byte[] ratchetSharedKey(int keyIndex);
    byte[] ratchetKey(String participantIdentity, int keyIndex);
    
    // Export keys
    byte[] exportKey(String participantIdentity);
    byte[] exportSharedKey(int keyIndex);
    
    // Key management
    int getLatestKeyIndex(String participantIdentity);
    void clearKeys(String participantIdentity);
    void clearAllKeys();
    
    // Mode
    boolean isSharedKeyMode();
}
```

## Security Considerations

### Key Distribution

The SDK does not handle key distribution. You must implement a secure key exchange mechanism:

- Out-of-band key sharing (QR code, secure messaging)
- Public key cryptography (Diffie-Hellman, RSA)
- Trusted key server
- Pre-shared keys

### Key Sizes

- Default key size: 32 bytes (256 bits) for AES-256
- IV size: 12 bytes (96 bits) for GCM
- GCM tag: 128 bits

### Best Practices

1. **Never hardcode keys** - Generate or receive keys securely
2. **Rotate keys regularly** - Use key ratcheting for forward secrecy
3. **Clear keys on disconnect** - Call `keyProvider.clearAllKeys()` when leaving
4. **Validate decryption** - Always check for null return from `decrypt()`
5. **Handle errors** - Listen for encryption errors via `E2EEListener`

## Example: Secure Chat

```java
public class SecureChat {
    private RtcClient client;
    private E2EEManager e2ee;
    
    public void connect(String url, String token, byte[] sharedKey) {
        // Setup E2EE
        BaseKeyProvider keyProvider = new BaseKeyProvider(true);
        keyProvider.setSharedKey(sharedKey, 0);
        E2EEOptions options = new E2EEOptions(keyProvider);
        e2ee = new E2EEManager(options);
        e2ee.setDataChannelEncryptionEnabled(true);
        
        // Connect
        client = new RtcClient();
        Room room = client.getRoom();
        
        room.addListener(new RoomListener() {
            @Override
            public void onConnected(Room r) {
                e2ee.setLocalIdentity(r.getLocalParticipant().getIdentity());
            }
            
            @Override
            public void onDataReceived(Room r, byte[] data,
                    RemoteParticipant participant, DataPacket.Kind kind, String topic) {
                if ("secure-chat".equals(topic)) {
                    EncryptedPacket packet = EncryptedPacket.fromBytes(data);
                    byte[] plaintext = e2ee.decrypt(participant.getIdentity(), packet);
                    if (plaintext != null) {
                        displayMessage(participant.getIdentity(), new String(plaintext));
                    }
                }
            }
        });
        
        client.connect(url, token);
    }
    
    public void sendSecureMessage(String message) {
        EncryptedPacket encrypted = e2ee.encrypt(message.getBytes());
        if (encrypted != null) {
            DataPacket packet = DataPacket.builder(encrypted.toBytes())
                .kind(DataPacket.Kind.RELIABLE)
                .topic("secure-chat")
                .build();
            client.publishData(packet);
        }
    }
    
    public void disconnect() {
        e2ee.cleanup();
        e2ee.getKeyProvider().clearAllKeys();
        client.disconnect();
    }
}
```

## Limitations

- **Media track E2EE not supported** - WebRTC FrameCryptor API not available in webrtc-java
- **No automatic key exchange** - Keys must be distributed manually
- **In-memory key storage** - BaseKeyProvider stores keys in memory only

## Next Steps

- [Data Channels](data-channels.md) - Send data messages
- [Publishing Media](publishing.md) - Publish audio/video
- [API Reference](api-reference.md) - Complete API documentation
