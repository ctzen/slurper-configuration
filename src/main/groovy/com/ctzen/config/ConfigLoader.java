package com.ctzen.config;

import groovy.util.ConfigObject;

import java.util.List;

/**
 * ConfigLoader loads configurations from a location.
 *
 * <p>
 * implements {@link org.springframework.context.ResourceLoaderAware} to have a
 * {@link org.springframework.core.io.ResourceLoader} injected.
 * </p>
 *
 * @author cchang
 */
public interface ConfigLoader {

    /**
     * @param location  resource location string
     * @return  true if the loader is capable of handle the location
     */
    boolean handles(String location);

    /**
     * Loads configurations from a location for the specified profiles.
     * <p>
     * If the config resource is missing, it should be skipped and an empty list returned.
     * </p>
     * <p>
     * Must be thread-safe.
     * </p>
     *
     * @param location  resource location string
     * @param profiles  config profile names (may be empty but never null)
     * @return list of {@link ConfigObject} (may be empty)
     */
    List<ConfigObject> load(String location, List<String> profiles);

}
