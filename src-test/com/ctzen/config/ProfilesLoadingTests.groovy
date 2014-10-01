package com.ctzen.config

import groovy.transform.CompileStatic

import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class ProfilesLoadingTests {

    void noProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.ProfilesLoadingTestsConfig')
        config.load()
        assert 'I am default' == config.get('foo')
        assert 'bar@root' == config.get('bar')
    }

    void devProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.ProfilesLoadingTestsConfig')
        config.setProfiles(ConfigProfile.DEV)
        config.load()
        assert 'I am dev' == config.get('foo')
        assert 'bar@root' == config.get('bar')

    }

    void prodProfile() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.ProfilesLoadingTestsConfig')
        config.setProfiles(ConfigProfile.PROD)
        config.load()
        assert 'I am prod' == config.get('foo')
        assert 'bar@root' == config.get('bar')
    }

    void devProdProfiles() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.ProfilesLoadingTestsConfig')
        config.setProfiles(ConfigProfile.DEV, ConfigProfile.PROD)
        config.load()
        assert 'I am prod' == config.get('foo')
        assert 'bar@root' == config.get('bar')
    }

    void prodDevProfiles() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.ProfilesLoadingTestsConfig')
        config.setProfiles(ConfigProfile.PROD, ConfigProfile.DEV)
        config.load()
        assert 'I am dev' == config.get('foo')
        assert 'bar@root' == config.get('bar')
    }

}
