package io.livekit.sdk.e2ee;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Base implementation of KeyProvider with in-memory key storage. Supports both per-participant keys
 * and shared key mode. Based on LiveKit Android SDK's BaseKeyProvider.
 */
public class BaseKeyProvider implements KeyProvider {
  public static final String DEFAULT_RATCHET_SALT = "LKFrameEncryptionKey";
  public static final String DEFAULT_MAGIC_BYTES = "LK-ROCKS";
  public static final int DEFAULT_RATCHET_WINDOW_SIZE = 16;
  public static final int DEFAULT_KEY_SIZE = 32; // 256 bits

  private final Map<String, Map<Integer, KeyInfo>> participantKeys = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> latestKeyIndex = new ConcurrentHashMap<>();
  private final Map<Integer, KeyInfo> sharedKeys = new ConcurrentHashMap<>();
  private final AtomicInteger sharedKeyIndex = new AtomicInteger(0);
  private final SecureRandom random = new SecureRandom();
  private final boolean sharedKeyMode;
  private final byte[] ratchetSalt;
  private final int ratchetWindowSize;
  private String localIdentity;

  public BaseKeyProvider(boolean sharedKeyMode) {
    this(sharedKeyMode, DEFAULT_RATCHET_SALT, DEFAULT_RATCHET_WINDOW_SIZE);
  }

  public BaseKeyProvider(boolean sharedKeyMode, String ratchetSalt, int ratchetWindowSize) {
    this.sharedKeyMode = sharedKeyMode;
    this.ratchetSalt = ratchetSalt.getBytes();
    this.ratchetWindowSize = ratchetWindowSize;
  }

  public void setLocalIdentity(String identity) {
    this.localIdentity = identity;
  }

  @Override
  public KeyInfo getKey(String participantIdentity) {
    if (sharedKeyMode) {
      return sharedKeys.get(sharedKeyIndex.get());
    }
    Map<Integer, KeyInfo> keys = participantKeys.get(participantIdentity);
    if (keys == null || keys.isEmpty()) {
      return null;
    }
    int index = getLatestKeyIndex(participantIdentity);
    return keys.get(index);
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
    if (sharedKeyMode) {
      setSharedKey(key, keyIndex);
      return;
    }
    KeyInfo keyInfo = new KeyInfo(key, keyIndex, participantIdentity);
    participantKeys
        .computeIfAbsent(participantIdentity, k -> new ConcurrentHashMap<>())
        .put(keyIndex, keyInfo);
    latestKeyIndex
        .computeIfAbsent(participantIdentity, k -> new AtomicInteger(0))
        .updateAndGet(current -> Math.max(current, keyIndex));
  }

  @Override
  public KeyInfo rotateKey() {
    if (sharedKeyMode) {
      byte[] newKey = ratchetSharedKey(sharedKeyIndex.get());
      return sharedKeys.get(sharedKeyIndex.get());
    }
    String identity = localIdentity != null ? localIdentity : "local";
    byte[] newKey = ratchetKey(identity, getLatestKeyIndex(identity));
    return getKey(identity);
  }

  @Override
  public KeyInfo getSharedKey() {
    return sharedKeys.get(sharedKeyIndex.get());
  }

  @Override
  public void setSharedKey(byte[] key, int keyIndex) {
    KeyInfo keyInfo = new KeyInfo(key, keyIndex, "shared");
    sharedKeys.put(keyIndex, keyInfo);
    sharedKeyIndex.updateAndGet(current -> Math.max(current, keyIndex));
  }

  @Override
  public byte[] ratchetSharedKey(int keyIndex) {
    KeyInfo currentKey = sharedKeys.get(keyIndex);
    if (currentKey == null) {
      return null;
    }
    byte[] newKey = deriveKey(currentKey.getKey());
    int newIndex = keyIndex + 1;
    setSharedKey(newKey, newIndex);
    return newKey;
  }

  @Override
  public byte[] ratchetKey(String participantIdentity, int keyIndex) {
    KeyInfo currentKey = getKey(participantIdentity, keyIndex);
    if (currentKey == null) {
      return null;
    }
    byte[] newKey = deriveKey(currentKey.getKey());
    int newIndex = keyIndex + 1;
    setKey(newKey, newIndex, participantIdentity);
    return newKey;
  }

  @Override
  public byte[] exportKey(String participantIdentity) {
    KeyInfo key = getKey(participantIdentity);
    return key != null ? key.getKey() : null;
  }

  @Override
  public byte[] exportSharedKey(int keyIndex) {
    KeyInfo key = sharedKeys.get(keyIndex);
    return key != null ? key.getKey() : null;
  }

  @Override
  public int getLatestKeyIndex(String participantIdentity) {
    if (sharedKeyMode) {
      return sharedKeyIndex.get();
    }
    AtomicInteger index = latestKeyIndex.get(participantIdentity);
    return index != null ? index.get() : 0;
  }

  @Override
  public void clearKeys(String participantIdentity) {
    participantKeys.remove(participantIdentity);
    latestKeyIndex.remove(participantIdentity);
  }

  @Override
  public void clearAllKeys() {
    participantKeys.clear();
    latestKeyIndex.clear();
    sharedKeys.clear();
    sharedKeyIndex.set(0);
  }

  @Override
  public boolean isSharedKeyMode() {
    return sharedKeyMode;
  }

  /** Derive a new key from the current key using HKDF-like derivation. */
  private byte[] deriveKey(byte[] currentKey) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec keySpec = new SecretKeySpec(ratchetSalt, "HmacSHA256");
      mac.init(keySpec);
      return mac.doFinal(currentKey);
    } catch (Exception e) {
      // Fallback to simple key generation
      byte[] newKey = new byte[DEFAULT_KEY_SIZE];
      random.nextBytes(newKey);
      return newKey;
    }
  }

  /** Generate a new random key. */
  public byte[] generateKey() {
    byte[] key = new byte[DEFAULT_KEY_SIZE];
    random.nextBytes(key);
    return key;
  }

  public int getRatchetWindowSize() {
    return ratchetWindowSize;
  }
}
