package io.livekit.sdk.rtc;

/** Describes a shareable desktop source: a screen or an application window. */
public class DesktopSourceInfo {
  private final long id;
  private final String title;
  private final boolean window;

  public DesktopSourceInfo(long id, String title, boolean window) {
    this.id = id;
    this.title = title;
    this.window = window;
  }

  public long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  /** True for an application window, false for a full screen. */
  public boolean isWindow() {
    return window;
  }

  @Override
  public String toString() {
    return (window ? "window" : "screen") + ":" + id + " (" + title + ")";
  }
}
