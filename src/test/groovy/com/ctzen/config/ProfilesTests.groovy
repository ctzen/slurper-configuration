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

    private static List<String> effectiveProfiles(Config config) {
        Reporter.log('effectiveProfiles=' + config.effectiveProfiles)
        return config.effectiveProfiles
    }

    void noProfile() {
        final Config config = new Config()
        assert [] == effectiveProfiles(config)
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
        assert ['initial'] == effectiveProfiles(config)
        config.setProfiles(profiles as String[])
        assert profiles == effectiveProfiles(config)
    }

    @Test(dataProvider = 'setProfilesData')
    void setProfilesCollection(final List<String> profiles) {
        final Config config = new Config()
        // test initial profiles is replaced
        config.setProfiles('initial')
        assert ['initial'] == effectiveProfiles(config)
        config.setProfiles(profiles)
        assert profiles == effectiveProfiles(config)
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
        assert ['initial'] == effectiveProfiles(config)
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                profiles as String[]
            }
        }
        config.setEnvironment(env)
        assert profiles == effectiveProfiles(config)
    }

    void environmentProfilesTrumps() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == effectiveProfiles(config)
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                ['env'] as String[]
            }
        }
        // env trumps
        config.setEnvironment(env)
        assert ['env'] == effectiveProfiles(config)
        // and back
        config.setEnvironment(null)
        assert ['non-env'] == effectiveProfiles(config)
    }

    void environmentEmptyProfile() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == effectiveProfiles(config)
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                new String[0]
            }
        }
        config.setEnvironment(env)
        assert ['non-env'] == effectiveProfiles(config)
    }

    void environmentNullProfile() {
        final Config config = new Config()
        config.setProfiles('non-env')
        assert ['non-env'] == effectiveProfiles(config)
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                null
            }
        }
        config.setEnvironment(env)
        assert ['non-env'] == effectiveProfiles(config)
    }

    void combineProfiles() {
        final Config config = new Config()
        config.combineProfiles = true
        config.setProfiles('non-env')
        assert ['non-env'] == effectiveProfiles(config)
        final Environment env = new AbstractEnvironment() {
            @Override
            String[] getActiveProfiles() {
                ['env'] as String[]
            }
        }
        config.setEnvironment(env)
        // combined profiles
        assert ['env', 'non-env'] == effectiveProfiles(config)
    }

}
