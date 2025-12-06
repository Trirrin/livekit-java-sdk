package io.livekit.sdk.e2ee;

/**
 * Configuration options for E2EE.
 */
public class E2EEOptions {
    private final EncryptionType encryptionType;
    private final KeyProvider keyProvider;
    private final int ratchetWindowSize;
    private final int ratchetSalt;
    private final boolean failureTolerance;

    private E2EEOptions(Builder builder) {
        this.encryptionType = builder.encryptionType;
        this.keyProvider = builder.keyProvider;
        this.ratchetWindowSize = builder.ratchetWindowSize;
        this.ratchetSalt = builder.ratchetSalt;
        this.failureTolerance = builder.failureTolerance;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public KeyProvider getKeyProvider() {
        return keyProvider;
    }

    public int getRatchetWindowSize() {
        return ratchetWindowSize;
    }

    public int getRatchetSalt() {
        return ratchetSalt;
    }

    public boolean isFailureTolerance() {
        return failureTolerance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EncryptionType encryptionType = EncryptionType.GCM;
        private KeyProvider keyProvider;
        private int ratchetWindowSize = 16;
        private int ratchetSalt = 0;
        private boolean failureTolerance = false;

        public Builder encryptionType(EncryptionType type) {
            this.encryptionType = type;
            return this;
        }

        public Builder keyProvider(KeyProvider provider) {
            this.keyProvider = provider;
            return this;
        }

        public Builder ratchetWindowSize(int size) {
            this.ratchetWindowSize = size;
            return this;
        }

        public Builder ratchetSalt(int salt) {
            this.ratchetSalt = salt;
            return this;
        }

        public Builder failureTolerance(boolean tolerance) {
            this.failureTolerance = tolerance;
            return this;
        }

        public E2EEOptions build() {
            if (keyProvider == null) {
                throw new IllegalStateException("KeyProvider is required");
            }
            return new E2EEOptions(this);
        }
    }
}
