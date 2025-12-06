package io.livekit.sdk.signaling;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenParserTest {

  private static String buildTestToken(String payload) {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    String encodedPayload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return header + "." + encodedPayload + ".nosig";
  }

  @Test
  void testParseToken() {
    String payload =
        "{\"iss\":\"test_issuer\",\"sub\":\"user123\",\"name\":\"Test User\",\"exp\":9999999999}";
    String token = buildTestToken(payload);

    TokenParser.TokenClaims claims = TokenParser.parse(token);

    assertEquals("test_issuer", claims.getIssuer());
    assertEquals("user123", claims.getIdentity());
    assertEquals("Test User", claims.getName());
    assertFalse(claims.isExpired());
  }

  @Test
  void testGetRoomName() {
    String payload = "{\"sub\":\"user1\",\"video\":{\"room\":\"my-room\"}}";
    String token = buildTestToken(payload);

    String roomName = TokenParser.getRoomName(token);
    assertEquals("my-room", roomName);
  }

  @Test
  void testGetIdentity() {
    String payload = "{\"sub\":\"participant_id\"}";
    String token = buildTestToken(payload);

    String identity = TokenParser.getIdentity(token);
    assertEquals("participant_id", identity);
  }

  @Test
  void testVideoGrant() {
    String payload = "{\"sub\":\"user1\",\"video\":{\"room\":\"test-room\",\"roomJoin\":\"true\"}}";
    String token = buildTestToken(payload);

    TokenParser.TokenClaims claims = TokenParser.parse(token);
    TokenParser.VideoGrant grant = claims.getVideoGrant();

    assertNotNull(grant);
    assertEquals("test-room", grant.getRoom());
    assertTrue(grant.canJoin());
  }

  @Test
  void testInvalidToken() {
    assertThrows(IllegalArgumentException.class, () -> TokenParser.parse("invalid"));
    assertThrows(IllegalArgumentException.class, () -> TokenParser.parse("a.b"));
  }
}
