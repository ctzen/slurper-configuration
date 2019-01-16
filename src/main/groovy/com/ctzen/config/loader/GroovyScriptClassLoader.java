package com.ctzen.config.loader;

import groovy.util.ConfigObject;
import org.springframework.context.ResourceLoaderAware;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles {@code "class:"} prefix, loads from a Groovy script class.
 * <p>
 * e.g. {@code class:org.acme.MyConfig}
 * </p>
 *
 * @author cchang
 */
public class GroovyScriptClassLoader extends AbstractConfigLoader implements ResourceLoaderAware {

    /**
     * Resource location string prefix for Groovy script class.
     */
    public static final String LOCATION_PREFIX = "class:";

    /**
     * @param location  resource location string
     * @return {@code true} if {@code location} starts with {@link #LOCATION_PREFIX}
     */
    @Override
    public boolean handles(final String location) {
        return location.startsWith(LOCATION_PREFIX);
    }

    @Override
    public List<ConfigObject> load(final String location, final List<String> profiles) {
        logLoading(location);
        final Class<?> scriptClass = loadClass(location.substring(LOCATION_PREFIX.length()));
        return scriptClass == null ? NO_CONFIG
                                   : slurpers(profiles).map(slurper -> slurper.parse(scriptClass))
                                                       .collect(Collectors.toList());
    }

}
