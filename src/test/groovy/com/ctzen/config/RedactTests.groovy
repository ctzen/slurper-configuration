package com.ctzen.config


import groovy.transform.CompileStatic
import org.testng.Reporter
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
/**
 * @author cchang
 */
@CompileStatic
@Test
class RedactTests {

    @BeforeClass
    void setup() {
        config = new Config()
        config.setLocations('class:com.ctzen.config.RedactTestsConfig')
        config.addRedactKeys('redactedByName')
        config.load()
        assert !config.empty
    }

    private Config config

    @DataProvider(name = 'simpleGetData')
    private Object[][] simpleGetData() {
        [
            [ 'clear', 'you can see me' ],
            [ 'redactedByConfig', 'redacted by config' ],
            [ 'redactedByName', 'redacted by name' ]
        ] as Object[][]
    }

    @Test(dataProvider = 'simpleGetData')
    void simpleGet(final String key, final Object expected) {
        assert config.containsKey(key)
        assert expected == config.get(key)
    }

    void redactKeys() {
        Reporter.log(config.redactKeys as String)
        assert ['redactedByConfig', 'redactedByName'] as Set == config.redactKeys
    }

}
