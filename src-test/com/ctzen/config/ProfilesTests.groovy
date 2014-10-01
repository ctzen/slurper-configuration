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
class ProfilesTests {

    void noProfile() {
        final Config config = new Config()
        Reporter.log('effectiveProfiles=' + config.effectiveProfiles)
        assert [] == config.effectiveProfiles
    }

    @DataProvider(name = 'setProfilesData')
    private Object[][] setProfilesData() {
        [
            [ [] ],
            [ ['foo'] ],
            [ ['foo', 'bar'] ],
            [ ['foo', 'bar', 'qux' ] ]
        ] as Object[][]
    }

    @Test(dataProvider = 'setProfilesData')
    void setProfilesStrings(final List<String> profiles) {
        final Config config = new Config()
        // test initial profiles is replaced
        config.setProfiles('initial')
        assert ['initial'] == config.effectiveProfiles
        config.setProfiles(profiles as String[])
        Reporter.log('effectiveProfiles=' + config.effectiveProfiles)
        assert profiles == config.effectiveProfiles
    }

    @Test(dataProvider = 'setProfilesData')
    void setProfilesCollection(final List<String> profiles) {
        final Config config = new Config()
        // test initial profiles is replaced
        config.setProfiles('initial')
        assert ['initial'] == config.effectiveProfiles
        config.setProfiles(profiles)
        Reporter.log('effectiveProfiles=' + config.effectiveProfiles)
        assert profiles == config.effectiveProfiles
    }

    @Test(dataProvider = 'setProfilesData')
    void setEnvironment(final List<String> profiles) {
        final Config config = new Config()
        // test initial profiles is replaced
        final Environment initialEnv = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                ['initial'] as String[]
            }
        }
        config.setEnvironment(initialEnv)
        assert ['initial'] == config.effectiveProfiles
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                profiles as String[]
            }
        }
        config.setEnvironment(env)
        Reporter.log('effectiveProfiles=' + config.effectiveProfiles)
        assert profiles == config.effectiveProfiles
    }

    void environmentProfilesTrumps() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == config.effectiveProfiles
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                ['env'] as String[]
            }
        }
        // env trumps
        config.setEnvironment(env)
        assert ['env'] == config.effectiveProfiles
        // and back
        config.setEnvironment(null)
        assert ['non-env'] == config.effectiveProfiles
    }

    void environmentEmptyProfile() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == config.effectiveProfiles
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                new String[0]
            }
        }
        config.setEnvironment(env)
        assert ['non-env'] == config.effectiveProfiles
    }

    void environmentNullProfile() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == config.effectiveProfiles
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                null
            }
        }
        config.setEnvironment(env)
        assert ['non-env'] == config.effectiveProfiles
    }

}
