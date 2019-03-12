import com.ctzen.config.gradle.Config
import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.GroovyObject
import org.apache.commons.io.output.ByteArrayOutputStream
import org.gradle.process.internal.ExecException
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

plugins {
    groovy
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"         // releases
    id("com.jfrog.artifactory") version "4.9.0"     // snapshots
    id("nebula.optional-base") version "5.0.0"
}

val snapshot = false

group = "com.ctzen.config"
version = "2.0.0" + (if (snapshot) "-SNAPSHOT" else "")
description = "Application configuration backed by Groovy ConfigSlurper, plus support for Spring Framework placeholders, and @value annotations."

fun gitRev(): String {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        return String(stdout.toByteArray()).trim()
    }
    catch (e: ExecException) {
        return "UNKNOWN"
    }
}

val gitRev = gitRev()

object Pub {
    const val name = "slurper-configuration"
    object Github {
        const val repo = "ctzen/slurper-configuration"
        val url = "https://github.com/${repo}"
        val gitUrl = "${url}.git"
        val issuesUrl = "${url}/issues"
        const val changelog = "CHANGELOG.md"
    }
    object License {
        const val name = "MIT License"
        const val url = "https://opensource.org/licenses/MIT"
        const val distribution = "repo"
    }
    object Bintray {
        var user: String? = null
        var key: String? = null
        const val repo = "ctzen"
        const val license = "MIT"
        val labels = arrayOf("groovy", "configuration")
    }
}

Pub.Bintray.user = project.property("bintray.user") as String?
Pub.Bintray.key = project.property("bintray.key") as String?

repositories {
    jcenter()
}

dependencies {
    implementation(Config.Deps.libGroovy)
    implementation(Config.Deps.libGuava)
    implementation(Config.Deps.libSpringContext)
    implementation(Config.Deps.libSlf4j)
    testImplementation(Config.Deps.libTestNg)
    testImplementation(Config.Deps.libAssertj)
    testImplementation(Config.Deps.libSpringTest)
    testRuntimeOnly(Config.Deps.libReportNg)
    testRuntimeOnly(Config.Deps.libGuice) {
        because("libReportNg needs it.")
    }
    testRuntimeOnly(Config.Deps.libLogback)
    testRuntimeOnly(files("src/test/lib/slurper-configuration-test.jar"))   // for testing META-INF/slurper-configuration.properties

}

tasks.compileJava {
    options.compilerArgs.addAll(Config.javaCompilerArgs)
}

tasks.compileTestJava {
    options.compilerArgs.addAll(Config.javaCompilerArgs)
}

tasks.jar {
    manifest {
        attributes(
                "Implementation-Title" to project.description,
                "Implementation-Version" to project.version,
                "Git-Revision" to gitRev,
                "Source-Compatibility" to java.sourceCompatibility,
                "Target-Compatibility" to java.targetCompatibility,
                "Built-JDK" to System.getProperty("java.version"),
                "Built-VM" to System.getProperty("java.vm.name"),
                "Built-OS" to System.getProperty("os.name")
        )
    }
}

tasks.test {
    testLogging.showStandardStreams = true
    systemProperty("org.uncommons.reportng.stylesheet", "${rootDir}/src/test/resources/reportng-custom.css")
    useTestNG {
        listeners.add("org.uncommons.reportng.HTMLReporter")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets.main.get().allJava)
    classifier = "sources"
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.javadoc)
    classifier = "javadoc"
}

publishing {
    publications {
        create<MavenPublication>(Pub.name) {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            pom {
                description.set(project.description)
                url.set(Pub.Github.url)
                licenses {
                    license {
                        name.set(Pub.License.name)
                        url.set(Pub.License.url)
                        distribution.set(Pub.License.distribution)
                    }
                }
                developers {
                    developer {
                        id.set("ctzen")
                        name.set("Chiang Seng Chang")
                        email.set("cs+github@ctzen.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("${buildDir}/repo")
        }
    }
}

bintray {
    user = Pub.Bintray.user
    key = Pub.Bintray.key
    setPublications(Pub.name)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = Pub.Bintray.repo
        name = project.name
        desc = project.description
        websiteUrl = Pub.Github.url
        vcsUrl = Pub.Github.gitUrl
        issueTrackerUrl = Pub.Github.issuesUrl
        setLicenses(Pub.Bintray.license)
        setLabels(*Pub.Bintray.labels)
        githubRepo = Pub.Github.repo
        githubReleaseNotesFile = Pub.Github.changelog
    })
}

artifactory {
    setContextUrl("http://oss.jfrog.org")
    publish(delegateClosureOf<PublisherConfig>{
        repository(delegateClosureOf<GroovyObject> {
            setProperty("repoKey", if (snapshot) "oss-snapshot-local" else "oss-release-local")
            setProperty("username", Pub.Bintray.user)
            setProperty("password", Pub.Bintray.key)
//            setProperty("publishBuildInfo", false)
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", Pub.name)
        })
    })
}

tasks.wrapper {
    version = Config.Vers.gradle
    distributionType = Wrapper.DistributionType.ALL
}
