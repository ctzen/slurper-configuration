package com.ctzen.config

import groovy.transform.CompileStatic

import java.nio.file.Files

import org.testng.Reporter
import org.testng.annotations.Test

/**
 * @author cchang
 */
@CompileStatic
@Test
class ReloadTests {

    void loadFromClass() {
        final File f = File.createTempFile('slurper-configuration-test-', '.groovy')
        Reporter.log(f.canonicalPath)
        Files.write(f.toPath(), '''
foo = 'foo-initial'
bar = 'bar-initial'
'''.bytes)
        final Config config = new Config()
        config.setLocations("file:${f.canonicalPath}")
        config.load()
        assert 'foo-initial' == config.get('foo')
        assert 'bar-initial' == config.get('bar')
        Files.write(f.toPath(), '''
foo = 'foo-reloaded'
'''.bytes)
        config.load()
        assert 'foo-reloaded' == config.get('foo')
        assert !config.containsKey('bar')
    }

}
