package com.ctzen.config.loader;

import com.ctzen.config.exception.ConfigException;
import groovy.util.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Final catch-all, loads from a Groovy script resource URL.
 * <p>
 * e.g. {@code classpath:org/acme/my-config.gy}
 * </p>

 * @author cchang
 */
public class GroovyScriptResourceLoader extends AbstractConfigLoader implements ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyScriptResourceLoader.class);

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * @param location  location
     * @return true (this is the catch-all loader)
     */
    @Override
    public boolean handles(final String location) {
        return true;
    }

    @Override
    public List<ConfigObject> load(final String location, final List<String> profiles) {
        final URL url = getURL(location);
        return url == null ? Collections.emptyList()
                           : slurpers(profiles).map(slurper -> slurper.parse(url))
                                               .collect(Collectors.toList());
    }

    private URL getURL(final String location) {
        final Resource res = resourceLoader.getResource(location);
        if (!res.exists()) {
            LOG.warn("Skip non-existence resource: {}", res);
            return null;
        }
        if (!res.isReadable()) {
            LOG.warn("Skip not readable resource: {}", res);
            return null;
        }
        try {
            return res.getURL();
        }
        catch (final IOException e) {
            throw new ConfigException("Not expecting a bad URL from a readable resource: " + res, e);
        }
    }

}
