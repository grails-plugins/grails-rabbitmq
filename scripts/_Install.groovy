//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
//
//    ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

def installMsg = '''
******************************
*   RabbitMQ Plugin README   *
******************************

The RabbitMQ Plugin has been installed.  The plugin requires properties be
defined in grails-app/conf/Config.groovy.  Specifically, connection factory 
settings must be defined.  Example:

rabbitmq {
    connectionfactory {
        username = 'guest'
        password = 'guest'
        hostname = 'localhost'
        consumers = 5
    }
}

See the plugin documentation for more information.
'''

println installMsg