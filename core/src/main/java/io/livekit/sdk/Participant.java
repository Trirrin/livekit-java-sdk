package io.livekit.sdk;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a participant in a room.
 */
public abstract class Participant {
    protected final String sid;
    protected final String identity;
    protected String name;
    protected String metadata;
    protected Map<String, String> attributes;
    protected ConnectionQuality connectionQuality;
    protected boolean isSpeaking;
    protected long audioLevel;
    protected final Map<String, TrackPublication> trackPublications;

    protected Participant(String sid, String identity) {
        this.sid = sid;
        this.identity = identity;
        this.connectionQuality = ConnectionQuality.UNKNOWN;
        this.trackPublications = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
    }

    public String getSid() {
        return sid;
    }

    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    public ConnectionQuality getConnectionQuality() {
        return connectionQuality;
    }

    public void setConnectionQuality(ConnectionQuality connectionQuality) {
        this.connectionQuality = connectionQuality;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(boolean speaking) {
        isSpeaking = speaking;
    }

    public long getAudioLevel() {
        return audioLevel;
    }

    public void setAudioLevel(long audioLevel) {
        this.audioLevel = audioLevel;
    }

    public Map<String, TrackPublication> getTrackPublications() {
        return Collections.unmodifiableMap(trackPublications);
    }

    public TrackPublication getTrackPublication(String sid) {
        return trackPublications.get(sid);
    }

    protected void addTrackPublication(TrackPublication publication) {
        trackPublications.put(publication.getSid(), publication);
    }

    protected void removeTrackPublication(String sid) {
        trackPublications.remove(sid);
    }

    public TrackPublication getTrackPublicationBySource(TrackSource source) {
        for (TrackPublication pub : trackPublications.values()) {
            if (pub.getSource() == source) {
                return pub;
            }
        }
        return null;
    }
}
