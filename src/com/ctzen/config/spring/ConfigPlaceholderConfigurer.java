package com.ctzen.config.spring;

import java.util.Properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import com.ctzen.config.Config;

/**
 * Specialization of {@link PlaceholderConfigurerSupport} that resolves ${...} placeholders
 * within bean definition property values and {@code @Value} annotations against the current Spring
 * {@link Environment} and its {@link Config}.
 *
 * @author cchang
 */
public class ConfigPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

    public static final String CONFIG_PROPERTY_SOURCE_NAME = "configProperties";

    public ConfigPlaceholderConfigurer(final Config config) {
        this.config = config;
    }

    private final Config config;

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    private MutablePropertySources propertySources;

    private PropertySources appliedPropertySources;

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
        if (propertySources == null) {
            propertySources = new MutablePropertySources();
            if (environment != null) {
                propertySources.addLast(
                    new PropertySource<Environment>(PropertySourcesPlaceholderConfigurer.ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, environment) {
                        @Override
                        public String getProperty(final String key) {
                            return source.getProperty(key);
                        }
                    }
                );
            }
            ConfigPropertySource configPropertySource = new ConfigPropertySource(CONFIG_PROPERTY_SOURCE_NAME, config);
            if (localOverride) {
                propertySources.addFirst(configPropertySource);
            }
            else {
                propertySources.addLast(configPropertySource);
            }
        }
        processProperties(beanFactory, new PropertySourcesPropertyResolver(propertySources));
        appliedPropertySources = propertySources;
    }

    protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final ConfigurablePropertyResolver propertyResolver) {
        propertyResolver.setPlaceholderPrefix(placeholderPrefix);
        propertyResolver.setPlaceholderSuffix(placeholderSuffix);
        propertyResolver.setValueSeparator(valueSeparator);
        StringValueResolver valueResolver = new StringValueResolver() {
            @Override
            public String resolveStringValue(final String strVal) {
                String resolved = ignoreUnresolvablePlaceholders ? propertyResolver.resolvePlaceholders(strVal) : propertyResolver.resolveRequiredPlaceholders(strVal);
                return resolved.equals(nullValue) ? null : resolved;
            }
        };
        doProcessProperties(beanFactoryToProcess, valueResolver);
    }

    /**
     * Implemented for compatibility with {@link org.springframework.beans.factory.config.PlaceholderConfigurerSupport}.
     * @deprecated in favor of {@link #processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)}
     * @throws UnsupportedOperationException
     */
    @Override
    @Deprecated
    protected void processProperties(final ConfigurableListableBeanFactory beanFactory, final Properties props) {
        throw new UnsupportedOperationException("Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
    }

    /**
     * Returns the property sources that were actually applied during
     * {@link #postProcessBeanFactory(ConfigurableListableBeanFactory) post-processing}.
     * @return the property sources that were applied
     * @throws IllegalStateException if the property sources have not yet been applied
     * @since 4.0
     */
    public PropertySources getAppliedPropertySources() {
        Assert.state(appliedPropertySources != null, "PropertySources have not get been applied");
        return appliedPropertySources;
    }

}
