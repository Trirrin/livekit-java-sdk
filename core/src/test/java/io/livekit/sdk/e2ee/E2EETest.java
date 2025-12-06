package io.livekit.sdk.e2ee;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class E2EETest {
  private BaseKeyProvider keyProvider;
  private E2EEManager e2eeManager;

  @BeforeEach
  void setUp() {
    keyProvider = new BaseKeyProvider(true);
    E2EEOptions options = new E2EEOptions(EncryptionType.GCM, keyProvider);
    e2eeManager = new E2EEManager(options);
  }

  @Test
  void testSetAndGetSharedKey() {
    byte[] key = new byte[32];
    for (int i = 0; i < 32; i++) {
      key[i] = (byte) i;
    }

    keyProvider.setSharedKey(key, 0);
    KeyInfo retrieved = keyProvider.getSharedKey();

    assertNotNull(retrieved);
    assertArrayEquals(key, retrieved.getKey());
    assertEquals(0, retrieved.getKeyIndex());
  }

  @Test
  void testKeyRatcheting() {
    byte[] initialKey = keyProvider.generateKey();
    keyProvider.setSharedKey(initialKey, 0);

    byte[] ratchetedKey = keyProvider.ratchetSharedKey(0);

    assertNotNull(ratchetedKey);
    assertFalse(java.util.Arrays.equals(initialKey, ratchetedKey));
    assertEquals(1, keyProvider.getLatestKeyIndex("shared"));
  }

  @Test
  void testDataPacketEncryptDecrypt() {
    byte[] key = new byte[32];
    for (int i = 0; i < 32; i++) {
      key[i] = (byte) i;
    }
    keyProvider.setSharedKey(key, 0);

    e2eeManager.setEnabled(true);
    e2eeManager.setDataChannelEncryptionEnabled(true);
    e2eeManager.setLocalIdentity("test-user");

    byte[] plaintext = "Hello, E2EE World!".getBytes();
    EncryptedPacket encrypted = e2eeManager.encrypt(plaintext);

    assertNotNull(encrypted);
    assertNotNull(encrypted.getPayload());
    assertNotNull(encrypted.getIv());
    assertEquals(0, encrypted.getKeyIndex());

    byte[] decrypted = e2eeManager.decrypt("test-user", encrypted);

    assertNotNull(decrypted);
    assertArrayEquals(plaintext, decrypted);
  }

  @Test
  void testEncryptionDisabled() {
    e2eeManager.setEnabled(false);

    byte[] plaintext = "Test".getBytes();
    EncryptedPacket encrypted = e2eeManager.encrypt(plaintext);

    assertNull(encrypted);
  }

  @Test
  void testDataChannelEncryptionDisabled() {
    e2eeManager.setEnabled(true);
    e2eeManager.setDataChannelEncryptionEnabled(false);

    byte[] plaintext = "Test".getBytes();
    EncryptedPacket encrypted = e2eeManager.encrypt(plaintext);

    assertNull(encrypted);
  }

  @Test
  void testPerParticipantKeys() {
    BaseKeyProvider perParticipant = new BaseKeyProvider(false);

    byte[] aliceKey = perParticipant.generateKey();
    byte[] bobKey = perParticipant.generateKey();

    perParticipant.setKey(aliceKey, 0, "alice");
    perParticipant.setKey(bobKey, 0, "bob");

    KeyInfo aliceRetrieved = perParticipant.getKey("alice", 0);
    KeyInfo bobRetrieved = perParticipant.getKey("bob", 0);

    assertNotNull(aliceRetrieved);
    assertNotNull(bobRetrieved);
    assertArrayEquals(aliceKey, aliceRetrieved.getKey());
    assertArrayEquals(bobKey, bobRetrieved.getKey());
    assertFalse(java.util.Arrays.equals(aliceKey, bobKey));
  }

  @Test
  void testE2EEListener() {
    final boolean[] stateChanged = {false};
    final int[] keySetIndex = {-1};

    e2eeManager.addListener(
        new E2EEListener() {
          @Override
          public void onE2EEStateChanged(boolean enabled) {
            stateChanged[0] = true;
          }

          @Override
          public void onKeySet(int keyIndex) {
            keySetIndex[0] = keyIndex;
          }
        });

    e2eeManager.setEnabled(false);
    assertTrue(stateChanged[0]);

    byte[] key = new byte[32];
    e2eeManager.setSharedKey(key, 5);
    assertEquals(5, keySetIndex[0]);
  }

  @Test
  void testRotateKey() {
    byte[] initialKey = keyProvider.generateKey();
    keyProvider.setSharedKey(initialKey, 0);

    KeyInfo rotated = e2eeManager.rotateKey();

    assertNotNull(rotated);
    assertEquals(1, rotated.getKeyIndex());
  }

  @Test
  void testEncryptedPacketImmutability() {
    byte[] payload = {1, 2, 3, 4};
    byte[] iv = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    EncryptedPacket packet = new EncryptedPacket(payload, iv, 0);

    payload[0] = 99;
    iv[0] = 99;

    assertNotEquals(99, packet.getPayload()[0]);
    assertNotEquals(99, packet.getIv()[0]);
  }
}
