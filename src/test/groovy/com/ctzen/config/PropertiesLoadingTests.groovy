package com.ctzen.config

import groovy.transform.CompileStatic
import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class PropertiesLoadingTests {

    void noProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.load()
        assert 'I am base properties' == config.get('foo')
    }

    void devProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.setProfiles(ConfigProfile.DEV)
        config.load()
        assert 'I am dev properties' == config.get('foo')

    }

    void prodProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.setProfiles(ConfigProfile.PROD)
        config.load()
        assert 'I am prod properties' == config.get('foo')
    }

    void devProdProfiles() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.setProfiles(ConfigProfile.DEV, ConfigProfile.PROD)
        config.load()
        assert 'I am prod properties' == config.get('foo')
    }

    void prodDevProfiles() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.setProfiles(ConfigProfile.PROD, ConfigProfile.DEV)
        config.load()
        assert 'I am dev properties' == config.get('foo')
    }

    // -qa.properties file does not exists, should skip successfully.
    void devQaProdProfiles() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.PropertiesLoadingTestsConfig')
        config.setLocations('classpath:config/test.properties')
        config.setProfiles(ConfigProfile.DEV, ConfigProfile.QA, ConfigProfile.PROD)
        config.load()
        assert 'I am prod properties' == config.get('foo')
    }

}
