package com.ctzen.config.spring;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import com.ctzen.config.Config;

/**
 * Spring {@link Config} {@link PropertySource} for {@link ConfigPlaceholderConfigurer}.
 *
 * @author cchang
 */
public class ConfigPropertySource extends EnumerablePropertySource<Config> {

    public ConfigPropertySource(final String name, final Config source) {
        super(name, source);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(source.keySet());
    }

    @Override
    public Object getProperty(final String name) {
        // defaultValue null allows @Value annotation to handle missing config
        return source.get(name, null);
    }

}
