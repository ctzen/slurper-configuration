package com.ctzen.config.loader;

import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import java.util.Collections;
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

    private static final Logger LOG = LoggerFactory.getLogger(GroovyScriptClassLoader.class);

    /**
     * Resource location string prefix for Groovy script class.
     */
    public static final String LOCATION_PREFIX = "class:";

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * @param location
     * @return {@code true} if {@code location} starts with {@link #LOCATION_PREFIX}
     */
    @Override
    public boolean handles(final String location) {
        return location.startsWith(LOCATION_PREFIX);
    }

    @Override
    public List<ConfigObject> load(final String location, final List<String> profiles) {
        final Class<?> scriptClass = loadClass(location.substring(LOCATION_PREFIX.length()));
        return scriptClass == null ? Collections.emptyList()
                                   : slurpers(profiles).map(slurper -> slurper.parse(scriptClass))
                                                       .collect(Collectors.toList());
    }

    private Class<?> loadClass(final String classname) {
        try {
            return resourceLoader.getClassLoader().loadClass(classname);
        }
        catch (final ClassNotFoundException e) {
            LOG.warn("Skip class not found: {}", classname);
            return null;
        }
    }

}
