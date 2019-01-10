package com.ctzen.config

import groovy.transform.CompileStatic

import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class ScriptValueTests {

    void scriptValues() {
        final Config config = new Config()
        config.setLocations(
            'classpath:config/script-value-tests-config.gy',
            'class:com.ctzen.config.ScriptValueTestsConfig'
        )
        config.load()
        assert [ 1, 3, 4, 5, 6 ] == config.get('foo')
        assert 'An orange a day' == config.get('ina.bar')
    }

}
