package com.ctzen.config

import groovy.transform.CompileStatic

import org.springframework.core.env.AbstractEnvironment
import org.springframework.core.env.Environment
import org.testng.Reporter
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class LocationsTests {

    void noLocation() {
        final Config config = new Config()
        Reporter.log('locations=' + config.locations)
        assert [] == config.locations
    }

    @DataProvider(name = 'setLocationsData')
    private Object[][] setLocationsData() {
        [
            [ [] ],
            [ ['foo'] ],
            [ ['foo', 'bar'] ],
            [ ['foo', 'bar', 'qux' ] ]
        ] as Object[][]
    }

    @Test(dataProvider = 'setLocationsData')
    void setLocationsStrings(final List<String> locations) {
        final Config config = new Config()
        // test initial locations is replaced
        config.setLocations('initial')
        assert ['initial'] == config.locations
        config.setLocations(locations as String[])
        Reporter.log('locations=' + config.locations)
        assert locations == config.locations
    }

    @Test(dataProvider = 'setLocationsData')
    void setLocationsCollection(final List<String> locations) {
        final Config config = new Config()
        // test initial locations is replaced
        config.setLocations('initial')
        assert ['initial'] == config.locations
        config.setLocations(locations)
        Reporter.log('locations=' + config.locations)
        assert locations == config.locations
    }

    void addLocations() {
        final Config config = new Config()
        // test initial locations is replaced
        config.setLocations('initial')
        assert ['initial'] == config.locations
        config.addLocations('foo', 'bar')
        Reporter.log('locations=' + config.locations)
        assert ['initial', 'foo', 'bar'] == config.locations
    }

}
