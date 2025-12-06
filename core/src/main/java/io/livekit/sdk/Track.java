package io.livekit.sdk;

/**
 * Represents a media track (audio, video, or data).
 */
public abstract class Track {
    protected final String sid;
    protected final String name;
    protected final TrackType type;
    protected TrackSource source;
    protected boolean muted;

    protected Track(String sid, String name, TrackType type) {
        this.sid = sid;
        this.name = name;
        this.type = type;
        this.source = TrackSource.UNKNOWN;
        this.muted = false;
    }

    public String getSid() {
        return sid;
    }

    public String getName() {
        return name;
    }

    public TrackType getType() {
        return type;
    }

    public TrackSource getSource() {
        return source;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
