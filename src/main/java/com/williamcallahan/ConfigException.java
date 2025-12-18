package com.williamcallahan;

/**
 * User-facing configuration error with a pre-formatted message.
 *
 * Caught at the top level to print cleanly without a stack trace.
 */
public final class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }
}
