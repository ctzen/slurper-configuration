package com.ctzen.config.loader;

import groovy.util.ConfigObject;
import org.springframework.context.ResourceLoaderAware;

import java.net.URL;
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

    /**
     * @param location  resource location string
     * @return true (this is the catch-all loader)
     */
    @Override
    public boolean handles(final String location) {
        return true;
    }

    @Override
    public List<ConfigObject> load(final String location, final List<String> profiles) {
        logLoading(location);
        final URL url = getResourceURL(location);
        return url == null ? NO_CONFIG
                           : slurpers(profiles).map(slurper -> slurper.parse(url))
                                               .collect(Collectors.toList());
    }

}
