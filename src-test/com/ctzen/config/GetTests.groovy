package com.ctzen.config

import groovy.transform.CompileStatic

import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import com.ctzen.config.exception.NoSuchKeyException

/**
 * @author cchang
 */
@CompileStatic
@Test
class GetTests {

    @BeforeClass
    void setup() {
        config = new Config()
        config.setLocations('class:com.ctzen.config.GetTestsConfig')
        config.load()
        assert !config.empty
    }

    private Config config

    @Test(expectedExceptions = NoSuchKeyException)
    void noSuchKey() {
        try {
            config.get('no.such.key')
        }
        catch (NoSuchKeyException e) {
            assert 'no.such.key' == e.key
            throw e
        }
    }

    void noSuchKeyDefault() {
        assert 123 == config.get('no.such.key', 123)
    }

    void nullValue() {
        assert null == config.get('aNull', 'Not me!')
    }

    @DataProvider(name = 'simpleGetData')
    private Object[][] simpleGetData() {
        [
            [ 'foo', 'I am foo' ],
            [ 'bar', 123 ],
            [ 'foo2', 'I am foo too' ],
            [ 'qux', 0 ],
            [ 'l1.qux', 1 ],
            [ 'l1.l2.qux', 2 ],
            [ 'l1.l2.l3.qux', 3 ],
            [ 'aNull', null ],
        ] as Object[][]
    }

    @Test(dataProvider = 'simpleGetData')
    void simpleGet(final String key, final Object expected) {
        assert config.containsKey(key)
        assert expected == config.get(key)
    }

    void pojo() {
        final TestPojo pojo = (TestPojo)config.get('pojo')
        assert null != pojo
        assert 'I am pojo!' == pojo.name
    }

}
