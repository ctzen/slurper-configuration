package com.ctzen.config.spring

import groovy.transform.CompileStatic

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import com.ctzen.config.Config
import com.ctzen.config.ConfigProfile

/**
 * @author cchang
 */
@CompileStatic
@Configuration
@Profile(ConfigProfile.UNIT_TEST)
class AppConfig {

    @Bean
    static Config config() {
        final Config ret = new Config()
        ret.setLocations('class:com.ctzen.config.spring.SpringTestsConfig')
        return ret
    }

    @Bean
    static ConfigPlaceholderConfigurer configPlaceholderConfigurer(final Config config) {
        new ConfigPlaceholderConfigurer(config)
    }

    @Bean
    Placeheld placeheld() {
        new Placeheld();
    }

}
