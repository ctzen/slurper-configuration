package com.ctzen.config.gradle

object Config {

    /**
     * Versions
     */
    object Vers {
        const val gradle = "5.1"
        const val spring = "5.1.4.RELEASE"
    }

    /**
     * Dependencies
     */
    object Deps {
        const val libGroovy = "org.codehaus.groovy:groovy-all:2.5.5"
        const val libGuava = "com.google.guava:guava:27.0.1-jre"
        // spring dependencies
        const val libSpringContext = "org.springframework:spring-context:${Vers.spring}"
        // logging dependencies
        const val libSlf4j = "org.slf4j:slf4j-api:1.7.25"
        // test dependencies
        const val libAssertj = "org.assertj:assertj-core:3.11.1"
        const val libTestNg = "org.testng:testng:6.14.3"
        const val libReportNg = "org.uncommons:reportng:1.1.4"
        const val libGuice = "com.google.inject:guice:4.2.2"   // reportng dependencies
        const val libLogback = "ch.qos.logback:logback-classic:1.2.3"
        const val libSpringTest = "org.springframework:spring-test:${Vers.spring}"
    }

/*
 dependencies {
     // logging
     runtime 'org.slf4j:jcl-over-slf4j:1.7.12'           // bridge commons-logging to slf4j
     runtime 'org.slf4j:log4j-over-slf4j:1.7.12'         // bridge log4j to slf4j
     // testing
     testCompile 'org.slf4j:jcl-over-slf4j:1.7.12'       // need this to compile groovy tests
 }

 configurations {
     all*.exclude group: 'commons-logging'       // exclude to use slf4j
     all*.exclude group: 'log4j'                 // exclude to use slf4j
 }
*/
    val javaCompilerArgs = arrayOf(
            "-Xlint:unchecked",
            "-Xlint:deprecation"
    )

}
