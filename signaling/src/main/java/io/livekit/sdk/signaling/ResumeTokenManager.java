package io.livekit.sdk.signaling;

/**
 * Manages resume tokens for reconnection.
 * Stores the latest token (either original or refreshed) for use during reconnection.
 */
public class ResumeTokenManager {
    private String currentToken;
    private String participantSid;
    private long tokenRefreshTime;

    public ResumeTokenManager(String initialToken) {
        this.currentToken = initialToken;
        this.tokenRefreshTime = System.currentTimeMillis();
    }

    public synchronized void updateToken(String newToken) {
        if (newToken != null && !newToken.isEmpty()) {
            this.currentToken = newToken;
            this.tokenRefreshTime = System.currentTimeMillis();
        }
    }

    public synchronized String getCurrentToken() {
        return currentToken;
    }

    public synchronized void setParticipantSid(String sid) {
        this.participantSid = sid;
    }

    public synchronized String getParticipantSid() {
        return participantSid;
    }

    public synchronized long getTokenAge() {
        return System.currentTimeMillis() - tokenRefreshTime;
    }

    public synchronized void reset() {
        this.currentToken = null;
        this.participantSid = null;
        this.tokenRefreshTime = 0;
    }
}
