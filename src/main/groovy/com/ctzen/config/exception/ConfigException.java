package com.ctzen.config.exception;

/**
 * Generic {@link com.ctzen.config.Config} exception.
 *
 * @author cchang
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
