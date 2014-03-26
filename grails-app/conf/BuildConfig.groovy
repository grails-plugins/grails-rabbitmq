grails.project.work.dir = 'target'
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
        compile ('org.springframework.amqp:spring-rabbit:1.2.1.RELEASE') {
            excludes 'junit',
                     'spring-aop',
                     'spring-core', // Use spring-core from Grails.
                     'spring-oxm',
                     'spring-test',
                     'spring-tx',
                     'slf4j-log4j12',
                     'log4j'
        }

        runtime "org.springframework.retry:spring-retry:1.0.3.RELEASE"
    }

    plugins {
        build ":release:2.0.4", {
            export = false
        }
    }
}
