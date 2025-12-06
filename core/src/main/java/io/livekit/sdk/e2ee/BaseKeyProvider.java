package io.livekit.sdk.e2ee;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base implementation of KeyProvider with in-memory key storage.
 * Supports both per-participant keys and shared key mode.
 */
public class BaseKeyProvider implements KeyProvider {
    private static final int DEFAULT_KEY_SIZE = 32; // 256 bits

    private final Map<String, Map<Integer, KeyInfo>> participantKeys = new ConcurrentHashMap<>();
    private final Map<Integer, KeyInfo> sharedKeys = new ConcurrentHashMap<>();
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);
    private final SecureRandom random = new SecureRandom();
    private final boolean sharedKeyMode;
    private String localIdentity;

    public BaseKeyProvider(boolean sharedKeyMode) {
        this.sharedKeyMode = sharedKeyMode;
    }

    public void setLocalIdentity(String identity) {
        this.localIdentity = identity;
    }

    @Override
    public KeyInfo getKey(String participantIdentity) {
        if (sharedKeyMode) {
            return sharedKeys.get(currentKeyIndex.get());
        }
        Map<Integer, KeyInfo> keys = participantKeys.get(participantIdentity);
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        return keys.get(currentKeyIndex.get());
    }

    @Override
    public KeyInfo getKey(String participantIdentity, int keyIndex) {
        if (sharedKeyMode) {
            return sharedKeys.get(keyIndex);
        }
        Map<Integer, KeyInfo> keys = participantKeys.get(participantIdentity);
        return keys != null ? keys.get(keyIndex) : null;
    }

    @Override
    public void setKey(byte[] key, int keyIndex, String participantIdentity) {
        KeyInfo keyInfo = new KeyInfo(key, keyIndex, participantIdentity);
        participantKeys.computeIfAbsent(participantIdentity, k -> new ConcurrentHashMap<>())
                .put(keyIndex, keyInfo);
        if (keyIndex > currentKeyIndex.get()) {
            currentKeyIndex.set(keyIndex);
        }
    }

    @Override
    public KeyInfo rotateKey() {
        int newIndex = currentKeyIndex.incrementAndGet();
        byte[] newKey = generateKey();
        String identity = localIdentity != null ? localIdentity : "local";

        KeyInfo keyInfo = new KeyInfo(newKey, newIndex, identity);
        if (sharedKeyMode) {
            sharedKeys.put(newIndex, keyInfo);
        } else {
            participantKeys.computeIfAbsent(identity, k -> new ConcurrentHashMap<>())
                    .put(newIndex, keyInfo);
        }
        return keyInfo;
    }

    @Override
    public KeyInfo getSharedKey() {
        return sharedKeys.get(currentKeyIndex.get());
    }

    @Override
    public void setSharedKey(byte[] key, int keyIndex) {
        KeyInfo keyInfo = new KeyInfo(key, keyIndex, "shared");
        sharedKeys.put(keyIndex, keyInfo);
        if (keyIndex > currentKeyIndex.get()) {
            currentKeyIndex.set(keyIndex);
        }
    }

    @Override
    public byte[] exportKey(String participantIdentity) {
        KeyInfo key = getKey(participantIdentity);
        return key != null ? key.getKey() : null;
    }

    @Override
    public void clearKeys(String participantIdentity) {
        participantKeys.remove(participantIdentity);
    }

    @Override
    public void clearAllKeys() {
        participantKeys.clear();
        sharedKeys.clear();
        currentKeyIndex.set(0);
    }

    private byte[] generateKey() {
        byte[] key = new byte[DEFAULT_KEY_SIZE];
        random.nextBytes(key);
        return key;
    }

    public int getCurrentKeyIndex() {
        return currentKeyIndex.get();
    }
}
