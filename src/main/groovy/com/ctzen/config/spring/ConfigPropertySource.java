package com.ctzen.config.spring;

import org.springframework.core.env.EnumerablePropertySource;

import com.ctzen.config.Config;

/**
 * Spring {@link Config} {@link org.springframework.core.env.PropertySource}
 * for {@link ConfigPlaceholderConfigurer}.
 *
 * @author cchang
 */
public class ConfigPropertySource extends EnumerablePropertySource<Config> {

    public ConfigPropertySource(final String name, final Config source) {
        super(name, source);
    }

    @Override
    public String[] getPropertyNames() {
        return source.keySet().toArray(new String[0]);
    }

    @Override
    public Object getProperty(final String name) {
        // defaultValue null allows @Value annotation to handle missing config
        return source.get(name, null);
    }

}
