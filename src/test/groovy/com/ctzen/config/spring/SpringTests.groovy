package com.ctzen.config.spring

import com.ctzen.config.Config
import com.ctzen.config.ConfigProfile
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test
/**
 * @author cchang
 */
@CompileStatic
@Test
@ContextConfiguration(classes = AppConfig)
@ActiveProfiles(ConfigProfile.UNIT_TEST)
class SpringTests extends AbstractTestNGSpringContextTests {

    @Autowired
    private Config config

    void simpleGet() {
        assert 'I am spring' == config.get('foo')
    }

    void profileOverrides() {
        assert 'bar@unittest' == config.get('bar')
    }

    @Autowired
    private Placeheld placeheld

    void placeheld() {
        assert 'John' == placeheld.name
        assert 42 == placeheld.meaning
        assert ['movies', 'comics'] as Set == placeheld.likes
        assert 'default' == placeheld.defa
    }

}
