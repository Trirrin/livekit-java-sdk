package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.media.video.VideoTrack;

/**
 * Wrapper for a local video track to be published.
 */
public class LocalVideoTrack {
    private final String id;
    private final String name;
    private final VideoTrack nativeTrack;
    private boolean muted;

    public LocalVideoTrack(String id, String name, VideoTrack nativeTrack) {
        this.id = id;
        this.name = name;
        this.nativeTrack = nativeTrack;
        this.muted = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VideoTrack getNativeTrack() {
        return nativeTrack;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (nativeTrack != null) {
            nativeTrack.setEnabled(!muted);
        }
    }

    public void dispose() {
        if (nativeTrack != null) {
            nativeTrack.dispose();
        }
    }
}
