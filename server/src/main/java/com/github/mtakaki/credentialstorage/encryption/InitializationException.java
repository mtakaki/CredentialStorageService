package com.github.mtakaki.credentialstorage.encryption;

/**
 * Exception thrown if the {@link EncryptionUtil} fails to initialize.
 *
 * @author mtakaki
 *
 */
public class InitializationException extends Exception {
    private static final long serialVersionUID = -4371171206245067083L;

    public InitializationException(final Exception exception) {
        super(exception);
    }
}