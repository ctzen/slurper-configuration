package com.ctzen.config.loader;

import com.ctzen.config.ConfigLoader;
import com.ctzen.config.exception.ConfigException;
import groovy.util.ConfigSlurper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author cchang
 */
public abstract class AbstractConfigLoader implements ConfigLoader {

    private ResourceLoader resourceLoader;

    /**
     * Since most {@link ConfigLoader} implementations requires a resource loader,
     * it is placed here in the abstract class.
     *
     * {@link ConfigLoader} implementations that requires a resource loader must
     * implement {@link org.springframework.context.ResourceLoaderAware}.
     *
     * @param resourceLoader    injected resource loader
     */
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * @return the injected resource loader, or null if concrete class does not implements {@link ResourceLoaderAware}.
     */
    protected ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    private void needResourceLoader() {
        if (resourceLoader == null) {
            throw new IllegalStateException("No resource loader, " + this.getClass().getCanonicalName()
                    + " must implements " + ResourceLoaderAware.class.getCanonicalName());
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Loads a class using the resource loader's class loader.
     *
     * @param classname     name of class to load
     * @return loaded class or null if not found
     */
    protected Class<?> loadClass(final String classname) {
        needResourceLoader();
        try {
            return resourceLoader.getClassLoader().loadClass(classname);
        }
        catch (final ClassNotFoundException e) {
            getLogger().warn("Skip class not found: {}", classname);
            return null;
        }
    }

    /**
     * Gets a {@link Resource} using the resource loader.
     *
     * @param location  resource location
     * @return the resource if it exists and is readable, otherwise null.
     */
    protected Resource getResource(final String location) {
        needResourceLoader();
        final Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            getLogger().warn("Skip non-existence resource: {}", resource);
            return null;
        }
        if (!resource.isReadable()) {
            getLogger().warn("Skip not readable resource: {}", resource);
            return null;
        }
        return resource;
    }

    /**
     * Gets a {@link Resource}'s URL using the resource loader.
     *
     * @param location  resource location
     * @return the resource's URL if it exists and is readable, otherwise null.
     */
    protected URL getResourceURL(final String location) {
        final Resource resource = getResource(location);
        try {
            return resource == null ? null : resource.getURL();
        }
        catch (final IOException e) {
            throw new ConfigException("Not expecting a bad URL from a readable resource: " + resource, e);
        }
    }

    /**
     * For concrete classes that needs {@link ConfigSlurper}.
     *
     * @param profiles  config profile names
     * @return stream of {@link ConfigSlurper} based on profile names
     */
    protected Stream<ConfigSlurper> slurpers(final List<String> profiles) {
        return profiles.isEmpty() ? Stream.of(new ConfigSlurper())
                                  : profiles.stream().map(ConfigSlurper::new);
    }

}
