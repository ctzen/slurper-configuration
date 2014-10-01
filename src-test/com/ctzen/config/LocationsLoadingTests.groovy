package com.ctzen.config

import groovy.transform.CompileStatic

import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class LocationsLoadingTests {

    void noLocation() {
        final Config config = new Config()
        config.load()
        assert 'I am meta-inf' == config.get('foo')
        assert !config.keySet().empty
        assert 0 < config.size()
    }

    void badLocations() {
        final Config config = new Config()
        config.setLocations(
            'class:no.such.Config',
            'classpath:no/such/config.gy'
        )
        config.load()
        assert 'I am meta-inf' == config.get('foo')
    }

    void loadFromClass() {
        final Config config = new Config()
        config.setLocations('class:com.ctzen.config.LocationsLoadingTestsConfig')
        config.load()
        assert 'I am class' == config.get('foo')
    }

    void loadFromScript() {
        final Config config = new Config()
        config.setLocations('classpath:com/ctzen/config/locations-loading-tests-config.gy')
        config.load()
        assert 'I am script' == config.get('foo')
    }

    void locationsOverrides() {
        final Config config = new Config()
        config.setLocations(
            'class:com.ctzen.config.LocationsLoadingTestsConfig',
            'classpath:com/ctzen/config/locations-loading-tests-config.gy'
        )
        config.load()
        assert 'I am script' == config.get('foo')
        assert 'bar from class' == config.get('bar')
        assert 'qux from script' == config.get('qux')
    }

}
