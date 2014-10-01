package com.ctzen.config;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.groovy.runtime.GStringImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.ctzen.config.exception.ConfigException;
import com.ctzen.config.exception.NoSuchKeyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * {@link ConfigSlurper} backed configuration.
 *
 * @author cchang
 */
public class Config implements InitializingBean, EnvironmentAware, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    public static final String LOCATION_PREFIX_CLASS = "class:";

    public static final String GROOVY_SCRIPT_VALUE_PREFIX = "groovy::";

    public static final String GROOVY_SCRIPT_BASE_VALUE_VAR = "x";

    private final Set<String> profiles = new LinkedHashSet<>();

    public void setProfiles(final String... profiles) {
        this.profiles.clear();
        for (final String profile: profiles) {
            this.profiles.add(profile);
        }
    }

    public void setProfiles(final Collection<String> profiles) {
        this.profiles.clear();
        this.profiles.addAll(profiles);
    }

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    /**
     * The final list of profiles depending on {@link #setProfiles(String...)}, {@link #setProfiles(Collection)}, and {@link #setEnvironment(Environment)}
     *
     * <p>
     * If {@link Environment} is set and {@link Environment#getActiveProfiles()} is not empty, use that.<br>
     * Otherwise, use the local profiles set by the {@code setProfiles(...)} methods.
     * </p>
     *
     * @return effective profile names
     */
    public List<String> getEffectiveProfiles() {
        Set<String> ret = null;
        if (environment != null) {
            final String[] envProfiles = environment.getActiveProfiles();
            if (envProfiles != null && envProfiles.length > 0) {
                ret = new LinkedHashSet<>();
                for (final String envProfile: envProfiles) {
                    ret.add(envProfile);
                }
            }
        }
        if (ret == null) {
            ret = profiles;
        }
        return ImmutableList.copyOf(ret);
    }

    private final List<String> locations = new ArrayList<>();

    public List<String> getLocations() {
        return ImmutableList.copyOf(locations);
    }

    /**
     * Set where to load the config from.
     *
     * <p>
     * Supports the spring-style resource strings, with an additional type of
     * {@code "class:fully.qualified.Classname"} which loads from a {@link Class}.
     * </p>
     *
     * @param locations
     */
    public void setLocations(final String... locations) {
        this.locations.clear();
        addLocations(locations);
    }

    /**
     * @see #setLocations(String...)
     */
    public void setLocations(final Collection<String> locations) {
        this.locations.clear();
        this.locations.addAll(locations);
    }

    public void addLocations(final String... locations) {
        for (final String location: locations) {
            this.locations.add(location);
        }
    }

    private static final String SLURPER_CONFIG_PROPERTIES_PATTERN = "classpath*:META-INF/slurper-configuration.properties";

    private List<String> getEffectiveLocations() {
        final List<String> ret = new ArrayList<>();
        // process META-INF/slurper-configuration.properties
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        final Resource[] resources;
        try {
            resources = resolver.getResources(SLURPER_CONFIG_PROPERTIES_PATTERN);
        }
        catch (IOException e) {
            throw new ConfigException("Error finding resources: " + SLURPER_CONFIG_PROPERTIES_PATTERN, e);
        }
        if (resources != null) {
            for (final Resource r: resources) {
                Properties props;
                try {
                    props = PropertiesLoaderUtils.loadProperties(r);
                }
                catch (IOException e) {
                    throw new ConfigException("Error loading resource: " + r, e);
                }
                final String propLocations = props.getProperty("locations");
                if (propLocations != null) {
                    for (String propLocation: propLocations.split(",")) {   // NOSONAR: Refactor this code to not nest more than 3 if/for/while/switch/try statements.
                                                                            // It's not that deep...
                        propLocation = propLocation.trim();
                        if (!propLocation.isEmpty()) {
                            ret.add(propLocation);
                        }
                    }
                }
            }
        }
        ret.addAll(locations);
        return ret;
    }

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * For spring to finalize this bean. Non spring users should call {@link #load()} instead.
     */
    // @PostConstruct does not work for static @Bean
    @Override
    public void afterPropertiesSet() {
        load();
    }

    /**
     * Load up this config using the configured locations and profiles.
     */
    public void load() {
        final long start = System.currentTimeMillis();
        if (resourceLoader == null) {
            resourceLoader = new DefaultResourceLoader(Thread.currentThread().getContextClassLoader());
        }
        values.clear();
        List<String> effectiveProfiles = getEffectiveProfiles();
        LOG.info("Load using profiles: {}", effectiveProfiles);
        final List<String> effectiveLocations = getEffectiveLocations();
        if (effectiveLocations.isEmpty()) {
            LOG.warn("No location to load!");
        }
        else {
            final ConfigObject configObject = new ConfigObject();
            for (final String location: effectiveLocations) {
                loadFromLocation(configObject, location, effectiveProfiles);
            }
            if (configObject.isEmpty()) {
                LOG.warn("Nothing was loaded!");
            }
            else {
                loadValues("", configObject);
            }
        }
        logValues();
        LOG.info("Loading took {}ms", System.currentTimeMillis() - start);
    }

    private void loadFromLocation(final ConfigObject configObject, final String location, final List<String> profiles) {
        if (profiles.isEmpty()) {
            final ConfigSlurper slurper = new ConfigSlurper();
            loadFromLocation(slurper, configObject, location);
        }
        else {
            for (final String profile: profiles) {
                final ConfigSlurper slurper = new ConfigSlurper(profile);
                loadFromLocation(slurper, configObject, location);
            }
        }
    }

    private void loadFromLocation(final ConfigSlurper slurper, final ConfigObject configObject, final String location) {
        LOG.info("Load from: {}", location);
        final ConfigObject cobj;
        if (location.startsWith(LOCATION_PREFIX_CLASS)) {
            cobj = loadFromClass(slurper, location.substring(LOCATION_PREFIX_CLASS.length()));
        }
        else {
            cobj = loadFromResource(slurper, location);
        }
        if (cobj != null) {
            resolveScriptValues(configObject, cobj);
            configObject.merge(cobj);
        }
    }

    private ConfigObject loadFromClass(final ConfigSlurper slurper, final String classname) {
        final Class<?> scriptClass;
        try {
            scriptClass = resourceLoader.getClassLoader().loadClass(classname);
        }
        catch (final ClassNotFoundException e) {
            LOG.warn("Class not found: {}", classname);
            return null;
        }
        return slurper.parse(scriptClass);
    }

    private ConfigObject loadFromResource(final ConfigSlurper slurper, final String location) {
        final Resource res = resourceLoader.getResource(location);
        if (!res.isReadable()) {
            LOG.warn("Resource not readable: {}", res);
            return null;
        }
        final URL url;
        try {
            url = res.getURL();
        }
        catch (IOException e) {
            throw new ConfigException("Not expecting a bad URL from a redable resource: " + res, e);
        }
        return slurper.parse(url);
    }

    /**
     * Resolve any inline groovy script values before merge.
     */
    private void resolveScriptValues(final ConfigObject base, final ConfigObject src) {
        @SuppressWarnings("unchecked")
        final Set<Entry<String, Object>> srcEntries = src.entrySet();
        for (final Entry<String, Object> srcEntry: srcEntries) {
            final String srcKey = srcEntry.getKey();
            final Object srcValue = srcEntry.getValue();
            if (srcValue instanceof ConfigObject) {
                final Object baseValue = base.get(srcKey);
                if (baseValue instanceof ConfigObject) {
                    resolveScriptValues((ConfigObject)baseValue, (ConfigObject)srcValue);
                }
            }
            else if (srcValue instanceof CharSequence) {
                final String sv = ((CharSequence)srcValue).toString();
                if (sv.startsWith(GROOVY_SCRIPT_VALUE_PREFIX)) {
                    final Binding binding = new Binding();
                    final Object baseValue = base.get(srcKey);
                    binding.setVariable(GROOVY_SCRIPT_BASE_VALUE_VAR, baseValue);
                    final GroovyShell shell = new GroovyShell(binding);
                    final String script = sv.substring(GROOVY_SCRIPT_VALUE_PREFIX.length());
                    final Object resolvedValue = shell.evaluate(script);
                    srcEntry.setValue(resolvedValue);
                }
            }
        }
    }

    /**
     * Flattens and finalizes the config values.
     */
    private void loadValues(final String keyPrefix, final ConfigObject configObject) {
        @SuppressWarnings("unchecked")
        final Set<Map.Entry<String,?>> entries = configObject.entrySet();
        for (final Map.Entry<String,?> entry: entries) {
            final String key = keyPrefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof ConfigObject) {
                loadValues(key + ".", (ConfigObject)value);
            }
            else {
                if (value instanceof GStringImpl) {
                    value = value.toString();
                }
                values.put(key, value);
            }
        }
    }

    private void logValues() {
        final StringBuilder msg = new StringBuilder(values.size() + " config values:");
        for (final Map.Entry<?, ?> entry: values.entrySet()) {
            msg.append("\n    ")
               .append(entry.getKey())
               .append(" (");
            final Object value = entry.getValue();
            if (value == null) {
                msg.append("null");
            }
            else {
                msg.append(value.getClass().getSimpleName());
            }
            msg.append(") = ")
               .append(entry.getValue());
        }
        LOG.info(msg.toString());
    }

    private final Map<String, Object> values = new TreeMap<>();

    /**
     * @return {@code true} if there is no config entry
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @return number of config entries
     */
    public int size() {
        return values.size();
    }

    /**
     * @param key   config key to search
     * @return {@code true} if there is a config entry of the {@code key}
     */
    public boolean containsKey(final String key) {
        return values.containsKey(key);
    }

    /**
     * @return all config keys
     */
    public Set<String> keySet() {
        return ImmutableSet.copyOf(values.keySet());
    }

    /**
     * Gets a config value.
     *
     * @param key   config key to search
     * @return config value associated with the {@code key}
     * @throws NoSuchKeyException if the {@code key} does not exists
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key) {
        if (!containsKey(key)) {
            throw new NoSuchKeyException(key);
        }
        return (T)values.get(key);
    }

    /**
     * Gets a config value, return the {@code defaultValue} if {@code key} does not exists.
     *
     * @param key   config key to search
     * @param defaultValue  return this if {@code key} does not exists
     * @return  config value associated with the {@code key}, or {@code defaultValue} if {@code key} does not exists
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final T defaultValue) {
        final T ret;
        if (containsKey(key)) {
            ret = (T)values.get(key);
        }
        else {
            ret = defaultValue;
        }
        return ret;
    }

}
