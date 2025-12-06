package io.livekit.sdk;

/**
 * Represents a video track.
 */
public class VideoTrack extends Track {
    private int width;
    private int height;

    public VideoTrack(String sid, String name) {
        super(sid, name, TrackType.VIDEO);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
