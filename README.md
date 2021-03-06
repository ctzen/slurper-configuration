# slurper-configuration

Application configuration backed by [ConfigSlurper](http://groovy.codehaus.org/ConfigSlurper),
plus support for [Spring Framework](http://projects.spring.io/spring-framework/) placeholders,
and [@Value](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Value.html) annotations.

## Features

#### More than Strings
Configure using actual typed values, not just Strings.
```groovy
day = 'Monday'                       // String, duh!
count = 123                          // a real int, no quotes!
vowels = [ 'a', 'e','i', 'o', 'u']   // a real List
now = new Date()                     // any Object!
// should be able to configure any type Groovy can muster.
```

#### Levels
Grouped and organized!
```groovy
// instead of
myService.foo = 1
myService.bar = 2

// do this
myService {
    foo = 1
    bar = 2
}
```

#### Profiles Support
Either by direct setting on the Config object, or automatically by Spring's [Environment](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/core/env/Environment.html) bean.
```groovy
target = 'http://default/'

environments {
    dev {
        target = 'http://localhost/'
    }
    qa {
        target = 'http://qa/'
    }
}
```

#### Local Overrides
By setting the locations to load on the Config object.  Latter locations will overrides values set by earlier locations.
Locations are Spring's resource strings such as `classpath:x/y/z`, or `file:/x/y/z`.
With an additional type `class:fully.qualified.Classname` which loads a compiled config groovy class.
```java
Config config = new Config();
config.setLocations(
    "class:conf.MyConfig",
    "file:/usr/local/etc/my-config.groovy"
);
```

#### ConfigPlaceholderConfigurer for Spring
Just like Spring's [PropertySourcesPlaceholderConfigurer](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/context/support/PropertySourcesPlaceholderConfigurer.html), 
it resolves `${...}` placeholders within bean definition property values and `@Value` annotations against the current Spring Environment, and the Config bean.
```java
public class MyBean {

    @Value("${day}")
    String day;
    
    @Value("${count}")
    int count;
    
    @Value("#{config.get("vowels")}")
    List<String> vowels;
    
    @Value("#{config.get('now')}")
    Date now;
}
```

## Setup and Usage

```kotlin
repositories {
    jcenter()
}
dependencies {
    implementation("com.ctzen.config:slurper-configuration:<version>")
}
```

### Standalone Setup
```java
// setup
Config config = new Config();
config.setProfiles('dev');  // optional
config.setLocations('class:conf/MyConfig');
config.load();
```

### Spring Context Setup
```java
@Configuration
public class AppConfig {

    @Bean
    public static Config config() {
        Config config = new Config();
        // no need to setProfiles(), typically set by Environment,
        // perhaps via the -Dspring.profiles.active parameter.
        config.setLocations(
        	"class:conf/MyConfig",
            "file:/usr/local/etc/my-local-config.groovy"
        );
        return config;
    }

    @Bean
    public static ConfigPlaceholderConfigurer configPlaceholderConfigurer(Config config) {
        return new ConfigPlaceholderConfigurer(config);
    }

}
```

### An example `conf/MyConfig.groovy`
```groovy
package conf

who = 'McGann'

life = 42

lorem {
	ipsum = """
Lorem ipsum dolor sit amet, nec primis argumentum an, nec integre eruditi laoreet eu. Eam illum nulla id, mea ea sonet alterum.
You can inject other var here like, the answer is ${life}.
"""
}

nemesis = [
    'Sherlock Holmes': 'James Moriarty',
    'Peter Pan': 'Captain Hook',
    'John McClane': 'Hans Gruber'
]
```

### Usage

#### Getters
```java
// get(key) will throw NoSuchKeyException if key is absent

String doctor = config.get("who");
int answer = config.get("life");
String gibbish = config.get("lorem.ipsum");
Map<String,String> archenimies = config.get("nemesis");

// get(key, default) will return the default if key is absent

int i = config.get("no.such.key", 7);    // i == 7
```

#### Spring [@Value](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Value.html) Annotation
```java
public class MyBean {

	// ${} may be used simple types

	@Value("${who}")
	String doctor;

	@Value("${life}")
	int answer;
    
    // #{} SpEL can be used for all types
    
    @Value("#{config.get('nemesis')}")
	Map<String,String> archenimies
}
```

## build

    ./gradlew clean build
    
## More Features

### .properties files as config source

Config location ends with `.properties` can be used as config sources.

Profiles are handled by file naming, for example:
```groovy
config.setProfiles('dev');
config.setLocations('classpath:conf/my-config.properties');
```

The following properties files (if exists) will be loaded in order:
```
1. classpath:conf/my-config.properties
2. classpath:conf/my-config@dev.properties
```

Essentially, the base config filename is appended with `@<profile name>` for each profile.

.properties files function the same as the other location types (such as overridding),
but only String values are supported (duh). 

#### Default Locations
Say you wish to use this in developing a library (jar), and you have some default configuration values, e.g. in your com.acme.AcmeConfig class, instead of asking your user to add that location, you can set it in `META-INF/slurper-configuration.properties` of your jar (ironic, I know). Here is an example:
```properties
# locations is comma separated and they are PREPENDED, i.e. loaded first.

locations=class:com.acme.AcmeConfig
```