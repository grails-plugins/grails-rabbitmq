grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits( "global" ) {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {        
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        runtime ('org.springframework.amqp:spring-rabbit:1.1.3.RELEASE') {
            excludes 'junit',
                     'spring-aop',
                     'spring-core', // Use spring-core from Grails.
                     'spring-oxm',
                     'spring-test',
                     'spring-tx',
                     'slf4j-log4j12',
                     'log4j'
        }

        runtime "org.springframework.retry:spring-retry:1.0.0.RELEASE"
    }

    plugins {
        build ":release:2.0.4", {
            export = false
        }
    }
}
