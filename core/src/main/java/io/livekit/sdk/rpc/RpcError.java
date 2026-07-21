package io.livekit.sdk.rpc;

import livekit.LivekitModels;

/**
 * Error passed across the wire for RPC failures. Codes 1000-1999 are reserved by the SDK; built-in
 * codes match the official LiveKit client SDKs.
 */
public class RpcError extends Exception {

  // Built-in error codes, matching official LiveKit SDKs
  public static final int APPLICATION_ERROR = 1500;
  public static final int CONNECTION_TIMEOUT = 1501;
  public static final int RESPONSE_TIMEOUT = 1502;
  public static final int RECIPIENT_DISCONNECTED = 1503;
  public static final int RESPONSE_PAYLOAD_TOO_LARGE = 1504;
  public static final int SEND_FAILED = 1505;
  public static final int UNSUPPORTED_METHOD = 1400;
  public static final int RECIPIENT_NOT_FOUND = 1401;
  public static final int REQUEST_PAYLOAD_TOO_LARGE = 1402;
  public static final int UNSUPPORTED_SERVER = 1403;
  public static final int UNSUPPORTED_VERSION = 1404;

  private final int code;
  private final String data;

  /**
   * Create an application-level RPC error. Use codes outside 1000-1999, which are reserved.
   *
   * @param code numeric error code transmitted to the caller
   * @param message human-readable error message
   * @param data optional application data transmitted to the caller
   */
  public RpcError(int code, String message, String data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  public RpcError(int code, String message) {
    this(code, message, null);
  }

  public int getCode() {
    return code;
  }

  public String getData() {
    return data;
  }

  public LivekitModels.RpcError toProto() {
    LivekitModels.RpcError.Builder builder =
        LivekitModels.RpcError.newBuilder()
            .setCode(code)
            .setMessage(getMessage() == null ? "" : getMessage());
    if (data != null) {
      builder.setData(data);
    }
    return builder.build();
  }

  public static RpcError fromProto(LivekitModels.RpcError proto) {
    return new RpcError(
        proto.getCode(), proto.getMessage(), proto.getData().isEmpty() ? null : proto.getData());
  }

  /** Create a built-in error with its standard message. */
  public static RpcError builtIn(int code) {
    return new RpcError(code, builtInMessage(code));
  }

  private static String builtInMessage(int code) {
    switch (code) {
      case APPLICATION_ERROR:
        return "Application error in method handler";
      case CONNECTION_TIMEOUT:
        return "Connection timeout";
      case RESPONSE_TIMEOUT:
        return "Response timeout";
      case RECIPIENT_DISCONNECTED:
        return "Recipient disconnected";
      case RESPONSE_PAYLOAD_TOO_LARGE:
        return "Response payload too large";
      case SEND_FAILED:
        return "Failed to send";
      case UNSUPPORTED_METHOD:
        return "Method not supported at destination";
      case RECIPIENT_NOT_FOUND:
        return "Recipient not found";
      case REQUEST_PAYLOAD_TOO_LARGE:
        return "Request payload too large";
      case UNSUPPORTED_SERVER:
        return "RPC not supported by server";
      case UNSUPPORTED_VERSION:
        return "Unsupported RPC version";
      default:
        return "RPC error";
    }
  }
}
