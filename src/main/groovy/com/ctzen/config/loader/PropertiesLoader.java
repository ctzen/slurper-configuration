package com.ctzen.config.loader;

import groovy.util.ConfigObject;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Handles {@code ".properties"} suffix, loads from a properties file.
 * <p>
 * e.g. {@code classpath:config/my-config.properties}
 * </p>
 * <p>
 * Profiles are handled by appending {@code "-<profile name>"} to the base name.
 * e.g. if profiles is set to {@code "prod"}, the properties files loaded are (in the order):
 * </p>
 * <ol>
 * <li>{@code config/my-config.properties}</li>
 * <li>{@code config/my-config-prod.properties}</li>
 * </ol>
 *
 * @author cchang
 */
public class PropertiesLoader extends AbstractConfigLoader implements ResourceLoaderAware {

    /**
     * Resource location string suffix for properties files.
     */
    public static final String LOCATION_SUFFIX = ".properties";

    /**
     * @param location  resource location string
     * @return {@code true} if {@code location} ends with {@link #LOCATION_SUFFIX}
     */
    @Override
    public boolean handles(final String location) {
        return location.endsWith(LOCATION_SUFFIX);
    }

    private static final String LOCATION_PROFILE_SEP = "@";

    @Override
    public List<ConfigObject> load(final String location, final List<String> profiles) {
        final List<ConfigObject> configObjects = new LinkedList<>();
        final ConfigObject baseConfigObject = loadProperties(location);
        if (baseConfigObject != null) {
            configObjects.add(baseConfigObject);
        }
        final String baseLocation = location.substring(0, location.length() - LOCATION_SUFFIX.length());
        profiles.stream()
                .map(profile -> loadProperties(baseLocation + LOCATION_PROFILE_SEP + profile + LOCATION_SUFFIX))
                .filter(Objects::nonNull)
                .forEach(configObjects::add);
        return configObjects;
    }

    private ConfigObject loadProperties(final String location) {
        logLoading(location);
        final Resource resource = getResource(location);
        if (resource == null) {
            return null;
        }
        final Properties properties;
        try {
            properties = PropertiesLoaderUtils.loadProperties(resource);
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
        final ConfigObject configObject = new ConfigObject();
        configObject.putAll(properties);
        return configObject;
    }

}
