package io.livekit.sdk.e2ee;

/**
 * Exception thrown when E2EE operations fail.
 */
public class E2EEException extends Exception {

    public E2EEException(String message) {
        super(message);
    }

    public E2EEException(String message, Throwable cause) {
        super(message, cause);
    }
}
