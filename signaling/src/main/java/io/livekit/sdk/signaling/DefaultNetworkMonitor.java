package io.livekit.sdk.signaling;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default network monitor using periodic connectivity checks. Checks connectivity by attempting to
 * connect to a well-known host.
 */
public class DefaultNetworkMonitor implements NetworkMonitor {
  private static final long CHECK_INTERVAL_MS = 5000;
  private static final int CONNECT_TIMEOUT_MS = 3000;
  private static final String CHECK_HOST = "dns.google";
  private static final int CHECK_PORT = 443;

  private final List<Listener> listeners = new CopyOnWriteArrayList<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final AtomicBoolean networkAvailable = new AtomicBoolean(true);
  private ScheduledFuture<?> checkTask;

  @Override
  public void start() {
    if (checkTask != null) {
      return;
    }
    checkTask =
        scheduler.scheduleAtFixedRate(
            this::checkConnectivity, 0, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    if (checkTask != null) {
      checkTask.cancel(false);
      checkTask = null;
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public boolean isNetworkAvailable() {
    return networkAvailable.get();
  }

  private void checkConnectivity() {
    boolean wasAvailable = networkAvailable.get();
    boolean isAvailable = performConnectivityCheck();
    networkAvailable.set(isAvailable);

    if (wasAvailable && !isAvailable) {
      notifyNetworkLost();
    } else if (!wasAvailable && isAvailable) {
      notifyNetworkAvailable();
    }
  }

  private boolean performConnectivityCheck() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(CHECK_HOST, CHECK_PORT), CONNECT_TIMEOUT_MS);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private void notifyNetworkAvailable() {
    for (Listener listener : listeners) {
      listener.onNetworkAvailable();
    }
  }

  private void notifyNetworkLost() {
    for (Listener listener : listeners) {
      listener.onNetworkLost();
    }
  }

  public void shutdown() {
    stop();
    scheduler.shutdown();
  }
}
