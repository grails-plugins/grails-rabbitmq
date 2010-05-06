import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter

class RabbitmqGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.0.RC2 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/services/*",
            "**/.gitignore"
    ]

    // TODO Fill in these fields
    def author = "Jeff Brown"
    def authorEmail = ""
    def title = "Rabbit MQ"
    def description = '''\\
The Rabbit MQ plugin provides integration with  the Rabbit MQ Messaging System.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rabbitmq"
    
    def loadAfter = ['services']

    private static LISTENER_CONTAINER_SUFFIX = '_MessageListenerContainer'

    def doWithSpring = {
        rabbitMQConnectionFactory(CachingConnectionFactory, 'localhost') {
            username = 'guest'
            password = 'guest'
            channelCacheSize = 10
        }
        application.serviceClasses.each { service ->
            def serviceClass = service.getClazz()
            def propertyName = service.propertyName

            def rabbitQueue = GCU.getStaticPropertyValue(serviceClass, 'rabbitQueue')
            if(rabbitQueue) { 
                "${propertyName}${LISTENER_CONTAINER_SUFFIX}"(SimpleMessageListenerContainer) {
                    connectionFactory = rabbitMQConnectionFactory
                    queueName = rabbitQueue
                    concurrentConsumers = 5
                    def adapter = new MessageListenerAdapter()
                    messageListener = adapter
                }
            }
        }
        
    }

    def doWithApplicationContext = { applicationContext ->
        def containerBeans = applicationContext.getBeansOfType(SimpleMessageListenerContainer)
        containerBeans.each { beanName, bean ->
            if(beanName.endsWith(LISTENER_CONTAINER_SUFFIX)) {
                def serviceName = beanName - LISTENER_CONTAINER_SUFFIX
                bean.messageListener.delegate = applicationContext.getBean(serviceName)
            }
        }
    }
}
