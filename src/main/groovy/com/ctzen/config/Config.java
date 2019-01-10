package com.ctzen.config;

import com.ctzen.config.exception.ConfigException;
import com.ctzen.config.exception.NoSuchKeyException;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
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

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link ConfigSlurper} backed configuration.
 *
 * @author cchang
 */
public class Config implements EnvironmentAware, ResourceLoaderAware, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    /**
     * Resource location string prefix for Groovy script class.
     */
    public static final String LOCATION_PREFIX_CLASS = "class:";

    /**
     * Inline Groovy script prefix.
     */
    public static final String GROOVY_SCRIPT_VALUE_PREFIX = "groovy::";

    /**
     * Inline Groovy script base value variable name.
     */
    public static final String GROOVY_SCRIPT_BASE_VALUE_VAR = "x";

    /*====================================================================================================
     * PROFILE NAMES
     *====================================================================================================*/

    private Environment environment;

    /**
     * @return environment or null
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Sets the environment.
     * Typically injected by Spring.
     *
     * @param environment   the environment
     */
    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    private final Set<String> profiles = new LinkedHashSet<>();     // maintain insertion order

    /**
     * @return profiles set by {@link #setProfiles(Collection)}, or {@link #setProfiles(String...)}.
     */
    public List<String> getProfiles() {
        return ImmutableList.copyOf(profiles);
    }

    /**
     * @param profiles  profile names
     */
    public void setProfiles(final Collection<String> profiles) {
        this.profiles.clear();
        this.profiles.addAll(profiles);
    }

    /**
     * @param profiles  profile names
     */
    public void setProfiles(final String... profiles) {
        setProfiles(Arrays.asList(profiles));
    }

    private boolean combineProfiles;

    /**
     * Should {@link #getEffectiveProfiles()} combine {@link Environment#getActiveProfiles()} and {@link #getProfiles()}?
     *
     * @return  {@code true} to combine, {@code false} to use {@link Environment#getActiveProfiles()} if present,
     *          and fallback to {@link #getProfiles()}.
     */
    public boolean isCombineProfiles() {
        return combineProfiles;
    }

    /**
     * Should {@link #getEffectiveProfiles()} combine {@link Environment#getActiveProfiles()} and {@link #getProfiles()}?
     *
     * @param combineProfiles   {@code true} to combine, {@code false} to use {@link Environment#getActiveProfiles()} if present,
     *                          and fallback to {@link #getProfiles()}.
     */
    public void setCombineProfiles(boolean combineProfiles) {
        this.combineProfiles = combineProfiles;
    }

    /**
     * The finalized list of effective profile names.
     *
     * <p>
     * If {@link #isCombineProfiles()} is {@code true}, combine {@link Environment#getActiveProfiles()} and {@link #getProfiles()}.
     * <br>
     * Otherwise, return {@link Environment#getActiveProfiles()} if present, and fallback to {@link #getProfiles()}.
     * </p>
     *
     * @return effective profile names
     */
    public List<String> getEffectiveProfiles() {
        Set<String> profiles = new LinkedHashSet<>();   // maintain insertion order
        if (environment != null) {
            String[] envProfiles = environment.getActiveProfiles();
            if (envProfiles != null) {  // envProfiles may be null
                profiles.addAll(Arrays.asList(envProfiles));
            }
        }
        if (profiles.isEmpty() || combineProfiles) {
            profiles.addAll(this.profiles);
        }
        return ImmutableList.copyOf(profiles);
    }

    /*====================================================================================================
     * LOCATIONS
     *====================================================================================================*/

    private final List<String> locations = new LinkedList<>();

    /**
     * @return  config resource locations (does not include those set by META-INF/slurper-configuration.properties)
     */
    public List<String> getLocations() {
        return ImmutableList.copyOf(locations);
    }

    /**
     * Set where to load the config from.
     *
     * <p>
     * Supports spring-style resource strings, e.g.<br>
     * &nbsp;&nbsp;{@code "classpath:org/acme/config.gy"}<br>
     * &nbsp;&nbsp;{@code "file:/path/to/config.gy"}
     * </p>
     *
     * <p>
     * Additional resource string types:
     * </p>
     *
     * <p>
     * {@code "class:fully.qualified.Classname"}
     * which loads from the compiled Groovy script class.
     * </p>
     *
     * @param locations     config locations
     */
    public void setLocations(final Collection<String> locations) {
        this.locations.clear();
        addLocations(locations);
    }

    /**
     * @param locations     config locations
     *
     * @see #setLocations(Collection)
     */
    public void setLocations(final String... locations) {
        setLocations(Arrays.asList(locations));
    }

    /**
     * Adds more config locations.
     *
     * @param locations     config locations
     */
    public void addLocations(final Collection<String> locations) {
        this.locations.addAll(locations);
    }

    /**
     * Adds more config locations.
     *
     * @param locations     config locations
     */
    public void addLocations(final String... locations) {
        addLocations(Arrays.asList(locations));
    }

    private List<String> getEffectiveLocations() {
        List<String> locations = new LinkedList<>();
        locations.addAll(getSlurperConfigPropertiesLocations());
        locations.addAll(this.locations);
        return ImmutableList.copyOf(locations);
    }

    /**
     * @return locations from META-INF/slurper-configuration.properties
     */
    private List<String> getSlurperConfigPropertiesLocations() {
        List<String> locations = new LinkedList<>();
        Arrays.stream(getSlurperConfigPropertiesResources())
                .forEach(resource -> getSlurperConfigPropertiesLocations(resource).forEach(locations::add));
        return locations;
    }

    private static final String SLURPER_CONFIG_PROPERTIES_PATTERN = "classpath*:META-INF/slurper-configuration.properties";

    private Resource[] getSlurperConfigPropertiesResources() {
        // load() would have resolved the resourceLoader
        final ResourceLoader resourceLoader = Objects.requireNonNull(this.resourceLoader);
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        try {
            return resolver.getResources(SLURPER_CONFIG_PROPERTIES_PATTERN);
        }
        catch (IOException e) {
            throw new ConfigException("Error finding resources: " + SLURPER_CONFIG_PROPERTIES_PATTERN, e);
        }
    }

    private static String trim2null(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private Stream<String> getSlurperConfigPropertiesLocations(Resource resource) {
        try {
            String locations = trim2null(PropertiesLoaderUtils.loadProperties(resource).getProperty("locations"));
            return locations == null
                 ? Stream.empty()
                 : StreamSupport.stream(Splitter.on(',').split(locations).spliterator(), false)
                    .map(Config::trim2null)
                    .filter(Objects::nonNull);
        }
        catch (IOException e) {
            throw new ConfigException("Error loading resource: " + resource, e);
        }
    }

    /*====================================================================================================
     * LOAD
     *====================================================================================================*/

    private ResourceLoader resourceLoader;

    /**
     * Specifies the {@link ResourceLoader} for loading config resources.<br>
     * Typically injected by Spring.<br>
     * Default is {@link DefaultResourceLoader} using the current thread context class loader.
     *
     * @param resourceLoader    a resource loader
     */
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
     * Lock and load this config object using the configured profiles and locations.
     */
    public void load() {
        final long start = System.currentTimeMillis();
        // finalize the resourceLoader
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
            effectiveLocations.forEach(location -> loadFromLocation(configObject, location, effectiveProfiles));
            if (configObject.isEmpty()) {
                LOG.warn("Nothing is loaded!");
            }
            else {
                putValues("", configObject);
            }
        }
        logLoadedValues();
        LOG.info("Loading took {}ms", System.currentTimeMillis() - start);
    }

    private void loadFromLocation(final ConfigObject configObject, final String location, final List<String> profiles) {
        LOG.info("Load from: {}", location);
        if (location.startsWith(LOCATION_PREFIX_CLASS)) {
            loadWithSlurper(configObject, location.substring(LOCATION_PREFIX_CLASS.length()), profiles, this::loadFromClass);
        }
        else {
            loadWithSlurper(configObject, location, profiles, this::loadFromResource);
        }
    }

    private void loadWithSlurper(final ConfigObject configObject, final String location, final List<String> profiles,
                                 final BiFunction<ConfigSlurper, String, ConfigObject> loader) {
        if (profiles.isEmpty()) {
            merge(configObject, loader.apply(new ConfigSlurper(), location));
        }
        else {
            profiles.forEach(profile -> merge(configObject, loader.apply(new ConfigSlurper(profile), location)));
        }
    }

    private ConfigObject loadFromClass(final ConfigSlurper slurper, final String classname) {
        final Class<?> scriptClass;
        try {
            scriptClass = resourceLoader.getClassLoader().loadClass(classname);
        }
        catch (final ClassNotFoundException e) {
            LOG.warn("Skip class not found: {}", classname);
            return null;
        }
        return slurper.parse(scriptClass);
    }

    private ConfigObject loadFromResource(final ConfigSlurper slurper, final String location) {
        final Resource res = resourceLoader.getResource(location);
        if (!res.exists()) {
            LOG.warn("Skip non-existence resource: {}", res);
            return null;
        }
        if (!res.isReadable()) {
            LOG.warn("Skip not readable resource: {}", res);
            return null;
        }
        final URL url;
        try {
            url = res.getURL();
        }
        catch (final IOException e) {
            throw new ConfigException("Not expecting a bad URL from a readable resource: " + res, e);
        }
        return slurper.parse(url);
    }

    private void merge(final ConfigObject base, final ConfigObject src) {
        if (src != null) {
            resolveScriptValues(base, src);
            base.merge(src);
        }
    }

    /**
     * Resolve any inline groovy script values before merge.
     */
    private void resolveScriptValues(final ConfigObject base, final ConfigObject src) {
        @SuppressWarnings("unchecked")
        final Set<Entry<String, Object>> srcEntries = src.entrySet();
        srcEntries.forEach(srcEntry -> {
            final String srcKey = srcEntry.getKey();
            final Object srcValue = srcEntry.getValue();
            if (srcValue instanceof ConfigObject) {
                final Object baseValue = base.get(srcKey);
                if (baseValue instanceof ConfigObject) {
                    resolveScriptValues((ConfigObject)baseValue, (ConfigObject)srcValue);
                }
            }
            else if (srcValue instanceof CharSequence) {
                final String sv = srcValue.toString();
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
        });
    }

    /**
     * Flattens and finalizes the config values.
     */
    private void putValues(final String keyPrefix, final ConfigObject configObject) {
        @SuppressWarnings("unchecked")
        final Set<Entry<String,?>> entries = configObject.entrySet();
        entries.forEach(entry -> {
            final String key = keyPrefix + entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof ConfigObject) {
                putValues(key + ".", (ConfigObject)value);
            }
            else {
                put(key, value);
            }
        });
    }

    private final Set<String> redactKeys = new HashSet<>();

    /**
     * @return keys of config values hidden when logging the loaded entries
     */
    public Set<String> getRedactKeys() {
        return ImmutableSet.copyOf(redactKeys);
    }

    /**
     * Redact sensitive config values (e.g. passwords) when logging the loaded entries.
     *
     * <p>
     * There are 2 ways to achieve this:
     * </p>
     *
     * <ol>
     *     <li>
     *         Use {@link Redact} in the config script.<br>
     *         {@code secret = new Redact('foobar')}
     *     </li>
     *     <li>
     *         Add the config keys here.<br>
     *         {@code config.addRedactKeys('secret')}
     *     </li>
     * </ol>
     *
     * @param redactKeys    config keys to redact the values
     */
    public void addRedactKeys(Collection<String> redactKeys) {
        this.redactKeys.addAll(redactKeys);
    }

    /**
     * @param redactKeys    config keys to redact the values
     *
     * @see #addRedactKeys(Collection)
     */
    public void addRedactKeys(String... redactKeys) {
        addRedactKeys(Arrays.asList(redactKeys));
    }

    private boolean logLoadedValues = true;

    /**
     * After loading, should the finalized config entries be logged?
     *
     * @return  {@code true} will log
     */
    public boolean isLogLoadedValues() {
        return logLoadedValues;
    }

    /**
     * After loading, should the finalized config entries be logged?
     *
     * <p>
     * Useful to know the actual config values.
     * </p>
     *
     * <p>
     * Default is {@code true}
     * </p>
     *
     * @param logLoadedValues   {@code true} to log
     */
    public void setLogLoadedValues(boolean logLoadedValues) {
        this.logLoadedValues = logLoadedValues;
    }

    private void logLoadedValues() {
        if (logLoadedValues) {
            final StringBuilder msg = new StringBuilder(values.size() + " config values:");
            values.forEach((key, value) -> {
                msg.append("\n    ").append(key);
                if (redactKeys.contains(key)) {
                    msg.append(" = <redacted>");
                }
                else {
                    msg.append(" (")
                       .append(value == null ? "null" : value.getClass().getSimpleName())
                       .append(") = ")
                       .append(value);
                }
            });
            LOG.info(msg.toString());
        }
    }

    /*====================================================================================================
     * CONFIG ENTRIES
     *====================================================================================================*/

    /**
     * THE loaded config entries.
     */
    private final Map<String, Object> values = new TreeMap<>();

    private void put(final String key, Object value) {
        if (value instanceof Redact) {
            addRedactKeys(key);
            value = ((Redact<?>)value).getValue();
        }
        if (value instanceof GStringImpl) {
            value = value.toString();
        }
        values.put(key, value);
    }

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
     *
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
     * @param <T>           value type
     *
     * @return config value associated with the {@code key}
     *
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
     * @param key           config key to search
     * @param defaultValue  return this if {@code key} does not exists
     * @param <T>           value type
     *
     * @return  config value associated with the {@code key}, or {@code defaultValue} if {@code key} does not exists
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key, final T defaultValue) {
        return containsKey(key) ? (T)values.get(key) : defaultValue;
    }

}
