package com.ctzen.config.loader;

import com.ctzen.config.ConfigLoader;
import groovy.util.ConfigSlurper;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author cchang
 */
public abstract class AbstractConfigLoader implements ConfigLoader {

    /**
     * @param profiles  config profile names
     * @return stream of {@link ConfigSlurper} based on profile names
     */
    protected Stream<ConfigSlurper> slurpers(final List<String> profiles) {
        return profiles.isEmpty() ? Stream.of(new ConfigSlurper())
                                  : profiles.stream().map(ConfigSlurper::new);
    }

}
