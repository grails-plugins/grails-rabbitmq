import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.grails.rabbitmq.AutoQueueMessageListenerContainer
import org.grails.rabbitmq.RabbitQueueBuilder
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

class RabbitmqGrailsPlugin {
    // the plugin version
    def version = "0.3-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
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
The Rabbit MQ plugin provides integration with the Rabbit MQ Messaging System.
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
        def connectionChannelCacheSize = connectionFactoryConfig?.channelCacheSize ?: 10
        def connectionFactoryConsumers = application.config.rabbitmq?.concurrentConsumers ?: 1
        
        if(!connectionFactoryUsername || !connectionFactoryPassword || !connectionFactoryHostname) {
            log.error 'RabbitMQ connection factory settings (rabbitmq.connectionfactory.username, rabbitmq.connectionfactory.password and rabbitmq.connectionfactory.hostname) must be defined in Config.groovy'
        } else {
          
            log.debug "Connecting to rabbitmq ${connectionFactoryUsername}@${connectionFactoryHostname} with ${connectionFactoryConsumers} consumers."
          
            def connectionFactoryClassName = connectionFactoryConfig?.className ?: 'org.springframework.amqp.rabbit.connection.CachingConnectionFactory'
            def parentClassLoader = getClass().classLoader
            def loader = new GroovyClassLoader(parentClassLoader)
            def connectionFactoryClass = loader.loadClass(connectionFactoryClassName)
            rabbitMQConnectionFactory(connectionFactoryClass, connectionFactoryHostname) {
                username = connectionFactoryUsername
                password = connectionFactoryPassword
                channelCacheSize = connectionChannelCacheSize
            }
            rabbitTemplate(RabbitTemplate) {
                connectionFactory = rabbitMQConnectionFactory
            }
            adm(org.grails.rabbitmq.GrailsRabbitAdmin, rabbitMQConnectionFactory)
            application.serviceClasses.each { service ->
                
                def serviceClass = service.clazz
                def propertyName = service.propertyName
        
                def rabbitQueue = GCU.getStaticPropertyValue(serviceClass, 'rabbitQueue')
                
                if(rabbitQueue) { 
                    "${propertyName}${LISTENER_CONTAINER_SUFFIX}"(SimpleMessageListenerContainer) {
                        // We manually start the listener once we have attached the
                        // service in doWithApplicationContext.
                        autoStartup = false
                        connectionFactory = rabbitMQConnectionFactory
                        concurrentConsumers = connectionFactoryConsumers
                        queueName = rabbitQueue
                    }
                }
                else {
                    def rabbitSubscribe = GCU.getStaticPropertyValue(serviceClass, 'rabbitSubscribe')
                    if (rabbitSubscribe) {
                        if (!(rabbitSubscribe instanceof CharSequence) && !(rabbitSubscribe instanceof Map)) {
                            log.error "The 'rabbitSubscribe' property on service ${service.fullName} must be a string or a map"
                        }
                        else {
                            "${propertyName}${LISTENER_CONTAINER_SUFFIX}"(AutoQueueMessageListenerContainer) {
                                // We manually start the listener once we have attached the
                                // service in doWithApplicationContext.
                                autoStartup = false
                                connectionFactory = rabbitMQConnectionFactory
                                concurrentConsumers = connectionFactoryConsumers
                                exchange = rabbitSubscribe
                            }
                        }
                    }
                }
            }
            
            def queuesConfig = application.config.rabbitmq?.queues
            if(queuesConfig) {
                def queueBuilder = new RabbitQueueBuilder()
                queuesConfig = queuesConfig.clone()
                queuesConfig.delegate = queueBuilder
                queuesConfig.resolveStrategy = Closure.DELEGATE_FIRST
                queuesConfig()
                
                // Deal with declared exchanges first.
                queueBuilder.exchanges?.each { exchange ->
                    if (log.debugEnabled) {
                        log.debug "Registering exchange '${exchange.name}'"
                    }
                    
                    "grails.rabbit.exchange.${exchange.name}"(exchange.type, exchange.name) {
                        durable = exchange.durable
                        autoDelete = exchange.autoDelete
                        arguments = exchange.arguments
                    }
                }
                
                // Next, the queues.
                queueBuilder.queues?.each { queue ->
                    if (log.debugEnabled) {
                        log.debug "Registering queue '${queue.name}'"
                    }
                    
                    "grails.rabbit.queue.${queue.name}"(Queue, queue.name) {
                        durable = queue.durable
                        autoDelete = queue.autoDelete
                        exclusive = queue.exclusive
                        arguments = queue.arguments
                    }
                }
                
                // Finally, the bindings between exchanges and queues.
                queueBuilder.bindings?.each { binding ->
                    if (log.debugEnabled) {
                        log.debug "Registering binding between exchange '${binding.exchange}' & queue '${binding.queue}'"
                    }
                    
                    def args = [ ref("grails.rabbit.exchange.${binding.exchange}"), ref ("grails.rabbit.queue.${binding.queue}") ]
                    if (binding.rule) {
                        log.debug "Binding with rule '${binding.rule}'"
                        args << binding.rule.toString()
                    }
                    
                    "grails.rabbit.binding.${binding.exchange}.${binding.queue}"(Binding, *args) {
                        arguments = binding.arguments
                    }
                }
            }
        }   
    }
    
    def doWithDynamicMethods = { appCtx ->
        addDynamicMessageSendingMethods application.allClasses, appCtx
    }
    
    private addDynamicMessageSendingMethods(classes, ctx) {
        if(ctx.rabbitMQConnectionFactory) {
            classes.each { clz ->
                clz.metaClass.rabbitSend = { Object[] args ->
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
                
                // Now that the listener is properly configured, we can start it.
                bean.start()
            }
        }
    }
    
    def onChange = { evt ->
        if(evt.source instanceof Class) {
            addDynamicMessageSendingMethods ([evt.source], evt.ctx)
        }
    }
}
