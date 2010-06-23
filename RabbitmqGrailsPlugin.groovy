import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

class RabbitmqGrailsPlugin {
    // the plugin version
    def version = "0.101"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/services/**",
            "grails-app/controllers/**",
            "grails-app/views/message/*",
            "grails-app/conf/Config.groovy",
            "src/groovy/org/grails/rabbitmq/test/**",
            "**/.gitignore"
    ]

    def author = "Jeff Brown"
    def authorEmail = "jeff.brown@springsource.com"
    def title = "Rabbit MQ"
    def description = '''\\
The Rabbit MQ plugin provides integration with  the Rabbit MQ Messaging System.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/rabbitmq"
    
    def loadAfter = ['services']
    def observe = ['*']

    private static LISTENER_CONTAINER_SUFFIX = '_MessageListenerContainer'

    def doWithSpring = { 
        
        def connectionFactoryConfig = application.config.rabbitmq?.connectionfactory
        
        def connectionFactoryUsername = connectionFactoryConfig?.username
        def connectionFactoryPassword = connectionFactoryConfig?.password
        def connectionFactoryHostname = connectionFactoryConfig?.hostname
        def connectionFactoryConsumers = connectionFactoryConfig?.consumers 
        
        if(!connectionFactoryUsername || !connectionFactoryPassword || !connectionFactoryHostname || !connectionFactoryConsumers) {
            log.error 'RabbitMQ connection factory settings (rabbitmq.connectionfactory.username, rabbitmq.connectionfactory.password, rabbitmq.connectionfactory.hostname and rabbitmq.connectionfactory.consumers) must be defined in Config.groovy'
        } else {
          
            log.debug "Connecting to rabbitmq ${connectionFactoryUsername}@${connectionFactoryHostname} with ${connectionFactoryConsumers} consumers."
          
            def connectionFactoryClassName = connectionFactoryConfig?.className ?: 'org.springframework.amqp.rabbit.connection.CachingConnectionFactory'
            def parentClassLoader = getClass().classLoader
            def loader = new GroovyClassLoader(parentClassLoader)
            def connectionFactoryClass = loader.loadClass(connectionFactoryClassName)
            rabbitMQConnectionFactory(connectionFactoryClass, connectionFactoryHostname) {
                username = connectionFactoryUsername
                password = connectionFactoryPassword
                channelCacheSize = 10
            }
            rabbitTemplate(RabbitTemplate) {
                connectionFactory = rabbitMQConnectionFactory
            }
            
            application.serviceClasses.each { service ->
                
                def serviceClass = service.clazz
                def propertyName = service.propertyName
        
                def rabbitQueue = GCU.getStaticPropertyValue(serviceClass, 'rabbitQueue')
                
                if(rabbitQueue) { 
                    "${propertyName}${LISTENER_CONTAINER_SUFFIX}"(SimpleMessageListenerContainer) {
                        connectionFactory = rabbitMQConnectionFactory
                        queueName = rabbitQueue
                        concurrentConsumers = connectionFactoryConsumers
                    }
                }
            }
        }   
    }
    
    def doWithDynamicMethods = { appCtx ->
        addDynamicMessageSendingMessages application.allClasses, appCtx
    }
    
    private addDynamicMessageSendingMessages(classes, ctx) {
        if(ctx.rabbitMQConnectionFactory) {
            classes.each { clz ->
                clz.metaClass.rabbitSend = { Object[] args ->
                    def connection = ctx.rabbitMQConnectionFactory.createConnection()
                    def channel = connection.createChannel()
                    channel.queueDeclare(args[-2])
                    channel.close()
                    connection.close()

                    if(args[-1] instanceof GString) { 
                        args[-1] = args[-1].toString()
                    }
                    ctx.rabbitTemplate.convertAndSend(*args)
                }
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        def containerBeans = applicationContext.getBeansOfType(SimpleMessageListenerContainer)
        containerBeans.each { beanName, bean ->
            if(beanName.endsWith(LISTENER_CONTAINER_SUFFIX)) {
                def adapter = new MessageListenerAdapter()
                def serviceName = beanName - LISTENER_CONTAINER_SUFFIX
                adapter.delegate = applicationContext.getBean(serviceName)
                bean.messageListener = adapter
            }
        }
    }
    
    def onChange = { evt ->
        if(evt.source instanceof Class) {
            addDynamicMessageSendingMessages ([evt.source], evt.ctx)
        }
    }
}
