package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCDataChannelState;
import dev.onvoid.webrtc.RTCPeerConnection;
import java.nio.ByteBuffer;
import livekit.LivekitModels;

/**
 * Manages WebRTC DataChannels for reliable and lossy data transport. LiveKit uses two channels: one
 * for reliable (ordered, guaranteed delivery) and one for lossy (unordered, no retransmission)
 * data.
 */
public class DataChannelManager {

  public static final String RELIABLE_CHANNEL_LABEL = "_reliable";
  public static final String LOSSY_CHANNEL_LABEL = "_lossy";

  private RTCDataChannel reliableChannel;
  private RTCDataChannel lossyChannel;
  private DataChannelListener listener;

  private volatile boolean reliableOpen = false;
  private volatile boolean lossyOpen = false;

  public DataChannelManager() {}

  /** Create data channels on the given peer connection. */
  public void createDataChannels(RTCPeerConnection peerConnection) {
    RTCDataChannelInit reliableInit = new RTCDataChannelInit();
    reliableInit.ordered = true;
    reliableChannel = peerConnection.createDataChannel(RELIABLE_CHANNEL_LABEL, reliableInit);
    setupChannelObserver(reliableChannel, true);

    RTCDataChannelInit lossyInit = new RTCDataChannelInit();
    lossyInit.ordered = false;
    lossyInit.maxRetransmits = 0;
    lossyChannel = peerConnection.createDataChannel(LOSSY_CHANNEL_LABEL, lossyInit);
    setupChannelObserver(lossyChannel, false);
  }

  /** Set up an incoming data channel from remote peer. */
  public void onDataChannel(RTCDataChannel channel) {
    String label = channel.getLabel();
    if (RELIABLE_CHANNEL_LABEL.equals(label)) {
      reliableChannel = channel;
      setupChannelObserver(channel, true);
    } else if (LOSSY_CHANNEL_LABEL.equals(label)) {
      lossyChannel = channel;
      setupChannelObserver(channel, false);
    }
  }

  private void setupChannelObserver(RTCDataChannel channel, boolean reliable) {
    channel.registerObserver(
        new RTCDataChannelObserver() {
          @Override
          public void onStateChange() {
            RTCDataChannelState state = channel.getState();
            if (state == RTCDataChannelState.OPEN) {
              if (reliable) {
                reliableOpen = true;
              } else {
                lossyOpen = true;
              }
              if (listener != null) {
                listener.onDataChannelOpen(reliable);
              }
            } else if (state == RTCDataChannelState.CLOSED) {
              if (reliable) {
                reliableOpen = false;
              } else {
                lossyOpen = false;
              }
            }
          }

          @Override
          public void onMessage(RTCDataChannelBuffer buffer) {
            if (listener != null) {
              byte[] data = new byte[buffer.data.remaining()];
              buffer.data.get(data);
              listener.onDataReceived(data, reliable);
            }
          }

          @Override
          public void onBufferedAmountChange(long previousAmount) {}
        });
  }

  /** Send data through the appropriate channel. */
  public boolean send(byte[] data, boolean reliable) {
    RTCDataChannel channel = reliable ? reliableChannel : lossyChannel;
    boolean isOpen = reliable ? reliableOpen : lossyOpen;

    if (channel == null || !isOpen) {
      return false;
    }

    try {
      ByteBuffer buffer = ByteBuffer.wrap(data);
      RTCDataChannelBuffer channelBuffer = new RTCDataChannelBuffer(buffer, true);
      channel.send(channelBuffer);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Send a protobuf DataPacket through the appropriate channel. */
  public boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable) {
    return send(packet.toByteArray(), reliable);
  }

  /** Check if reliable channel is open. */
  public boolean isReliableOpen() {
    return reliableOpen;
  }

  /** Check if lossy channel is open. */
  public boolean isLossyOpen() {
    return lossyOpen;
  }

  /** Set the data channel listener. */
  public void setListener(DataChannelListener listener) {
    this.listener = listener;
  }

  /** Close all data channels. */
  public void close() {
    if (reliableChannel != null) {
      reliableChannel.close();
      reliableChannel = null;
    }
    if (lossyChannel != null) {
      lossyChannel.close();
      lossyChannel = null;
    }
    reliableOpen = false;
    lossyOpen = false;
  }

  /** Listener for data channel events. */
  public interface DataChannelListener {
    /** Called when a data channel opens. */
    void onDataChannelOpen(boolean reliable);

    /** Called when data is received. */
    void onDataReceived(byte[] data, boolean reliable);
  }
}
