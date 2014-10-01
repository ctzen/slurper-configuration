package com.ctzen.config.exception;

import com.ctzen.config.Config;

/**
 * No such key in the {@link Config} object.
 *
 * @author cchang
 */
public class NoSuchKeyException extends ConfigException {

    private static final long serialVersionUID = 1L;

    public NoSuchKeyException(String key) {
        super("Missing config key '" + key + "'");
        this.key = key;
    }

    private final String key;

    /**
     * @return the missing config key
     */
    public String getKey() {
        return key;
    }

}
