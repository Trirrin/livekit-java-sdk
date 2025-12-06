package io.livekit.sdk.signaling;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class to parse LiveKit JWT tokens.
 * Extracts claims without signature verification.
 */
public class TokenParser {
    private static final Gson gson = new Gson();

    /**
     * Parsed token claims.
     */
    public static class TokenClaims {
        private String iss;      // Issuer (API key)
        private String sub;      // Subject (participant identity)
        private String name;     // Participant name
        private long exp;        // Expiration time
        private long nbf;        // Not before
        private long iat;        // Issued at
        private String jti;      // JWT ID
        private VideoGrant video; // Video grant

        public String getIssuer() {
            return iss;
        }

        public String getIdentity() {
            return sub;
        }

        public String getName() {
            return name;
        }

        public long getExpiration() {
            return exp;
        }

        public long getNotBefore() {
            return nbf;
        }

        public long getIssuedAt() {
            return iat;
        }

        public String getJwtId() {
            return jti;
        }

        public VideoGrant getVideoGrant() {
            return video;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > exp;
        }
    }

    /**
     * Video grant permissions.
     */
    public static class VideoGrant {
        private String room;
        private String roomJoin;
        private String roomList;
        private String roomRecord;
        private String roomAdmin;
        private String roomCreate;
        private String canPublish;
        private String canSubscribe;
        private String canPublishData;
        private String canUpdateOwnMetadata;
        private String hidden;
        private String recorder;

        public String getRoom() {
            return room;
        }

        public boolean canJoin() {
            return "true".equals(roomJoin) || "1".equals(roomJoin);
        }

        public boolean canPublish() {
            return !"false".equals(canPublish) && !"0".equals(canPublish);
        }

        public boolean canSubscribe() {
            return !"false".equals(canSubscribe) && !"0".equals(canSubscribe);
        }

        public boolean canPublishData() {
            return !"false".equals(canPublishData) && !"0".equals(canPublishData);
        }

        public boolean canUpdateOwnMetadata() {
            return "true".equals(canUpdateOwnMetadata) || "1".equals(canUpdateOwnMetadata);
        }

        public boolean isHidden() {
            return "true".equals(hidden) || "1".equals(hidden);
        }

        public boolean isRecorder() {
            return "true".equals(recorder) || "1".equals(recorder);
        }
    }

    /**
     * Parse a JWT token and extract claims.
     * Note: This does NOT verify the token signature.
     *
     * @param token JWT token string
     * @return Parsed token claims
     * @throws IllegalArgumentException if token format is invalid
     */
    public static TokenClaims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format");
        }

        String payload = parts[1];
        // Add padding if needed
        int padding = (4 - payload.length() % 4) % 4;
        payload = payload + "====".substring(0, padding);

        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        String json = new String(decoded, StandardCharsets.UTF_8);

        return gson.fromJson(json, TokenClaims.class);
    }

    /**
     * Extract the room name from a token.
     */
    public static String getRoomName(String token) {
        TokenClaims claims = parse(token);
        if (claims.getVideoGrant() != null) {
            return claims.getVideoGrant().getRoom();
        }
        return null;
    }

    /**
     * Extract the participant identity from a token.
     */
    public static String getIdentity(String token) {
        return parse(token).getIdentity();
    }
}
