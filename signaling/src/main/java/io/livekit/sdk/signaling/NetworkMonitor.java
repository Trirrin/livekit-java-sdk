package io.livekit.sdk.signaling;

/**
 * Interface for monitoring network connectivity changes. Implementations can detect network state
 * changes and trigger reconnection.
 */
public interface NetworkMonitor {

  /** Listener for network state changes. */
  interface Listener {
    void onNetworkAvailable();

    void onNetworkLost();
  }

  void start();

  void stop();

  void addListener(Listener listener);

  void removeListener(Listener listener);

  boolean isNetworkAvailable();
}
