import static org.springframework.amqp.core.Binding.DestinationType.QUEUE

import org.aopalliance.aop.Advice
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler
import org.grails.rabbitmq.RabbitConfigurationHolder
import org.grails.rabbitmq.RabbitDynamicMethods
import org.grails.rabbitmq.RabbitErrorHandler
import org.grails.rabbitmq.RabbitQueueBuilder
import org.grails.rabbitmq.RabbitServiceConfigurer
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptorFactoryBean
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.retry.MissingMessageIdAdvice
import org.springframework.amqp.support.converter.SimpleMessageConverter
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.MapRetryContextCache
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate


class RabbitmqGrailsPlugin {
    // the plugin version
    def version = "1.0.0"
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
    def title = "RabbitMQ Plugin"
    def description = "The RabbitMQ plugin provides integration with the RabbitMQ Messaging System."

    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [ [ name: "Peter Ledbrook", email: "pledbrook@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.codehaus.org/browse/GRAILSPLUGINS" ]
    def scm = [ url: "https://github.com/grails-plugins/grails-rabbitmq" ]

    // URL to the plugin's documentation
    def documentation = "http://grails-plugins.github.com/grails-rabbitmq/"
    
    def loadAfter = ['services']
    def observe = ['*']

    def doWithSpring = { 

        def rabbitmqConfig = application.config.rabbitmq
        def configHolder = new RabbitConfigurationHolder(rabbitmqConfig)

        def connectionFactoryConfig = rabbitmqConfig?.connectionfactory
        
        def connectionFactoryUsername = connectionFactoryConfig?.username
        def connectionFactoryPassword = connectionFactoryConfig?.password
        def connectionFactoryVirtualHost = connectionFactoryConfig?.virtualHost
        def connectionFactoryHostname = connectionFactoryConfig?.hostname
        def connectionChannelCacheSize = connectionFactoryConfig?.channelCacheSize ?: 10

        def messageConverterBean = rabbitmqConfig.messageConverterBean

        if(!connectionFactoryUsername || !connectionFactoryPassword || !connectionFactoryHostname) {
            log.error 'RabbitMQ connection factory settings (rabbitmq.connectionfactory.username, rabbitmq.connectionfactory.password and rabbitmq.connectionfactory.hostname) must be defined in Config.groovy'
        } else {
          
            log.debug "Connecting to rabbitmq ${connectionFactoryUsername}@${connectionFactoryHostname} with ${configHolder.getDefaultConcurrentConsumers()} consumers."
          
            def connectionFactoryClassName = connectionFactoryConfig?.className ?:
                    'org.springframework.amqp.rabbit.connection.CachingConnectionFactory'
            def parentClassLoader = getClass().classLoader
            def loader = new GroovyClassLoader(parentClassLoader)
            def connectionFactoryClass = loader.loadClass(connectionFactoryClassName)
            rabbitMQConnectionFactory(connectionFactoryClass, connectionFactoryHostname) {
                username = connectionFactoryUsername
                password = connectionFactoryPassword
                channelCacheSize = connectionChannelCacheSize

                if (connectionFactoryVirtualHost) {
                    virtualHost = connectionFactoryVirtualHost
                }
            }
            rabbitTemplate(RabbitTemplate) {
                connectionFactory = rabbitMQConnectionFactory
                if (messageConverterBean) {
                     messageConverter = ref(messageConverterBean)
                 } else {
                     def converter = new SimpleMessageConverter()
                     converter.createMessageIds = true
                     messageConverter = converter
                 }
            }
            adm(RabbitAdmin, rabbitMQConnectionFactory)
            rabbitErrorHandler(RabbitErrorHandler)

            // Add beans to hook up services as AMQP listeners.
            Set registeredServices = new HashSet()
            for(service in application.serviceClasses) {
                def serviceConfigurer = new RabbitServiceConfigurer(service, rabbitmqConfig)
                if(!serviceConfigurer.isListener() || !configHolder.isServiceEnabled(service)) continue

                def propertyName = service.propertyName
                if(!registeredServices.add(propertyName)) {
                    throw new IllegalArgumentException(
                            "Unable to initialize rabbitmq listeners properly." +
                            " More than one service named ${propertyName}.")
                }

                serviceConfigurer.configure(delegate)
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

                    "grails.rabbit.exchange.${exchange.name}"(exchange.type, exchange.name,
                            Boolean.valueOf(exchange.durable),
                            Boolean.valueOf(exchange.autoDelete),
                            exchange.arguments)
                }
                
                // Next, the queues.
                queueBuilder.queues?.each { queue ->
                    if (log.debugEnabled) {
                        log.debug "Registering queue '${queue.name}'"
                    }

                    "grails.rabbit.queue.${queue.name}"(Queue, queue.name,
                            Boolean.valueOf(queue.durable),
                            Boolean.valueOf(queue.exclusive),
                            Boolean.valueOf(queue.autoDelete),
                            queue.arguments,
                    )
                }
                
                // Finally, the bindings between exchanges and queues.
                queueBuilder.bindings?.each { binding ->
                    if (log.debugEnabled) {
                        log.debug "Registering binding between exchange '${binding.exchange}' & queue '${binding.queue}'"
                    }
                    
                    def args = [ ref("grails.rabbit.exchange.${binding.exchange}"), ref ("grails.rabbit.queue.${binding.queue}") ]
                    if (binding.rule) {
                        log.debug "Binding with rule '${binding.rule}'"

                        // Support GString and String for the rule. Other types of rule (Map
                        // is the only valid option atm) are passed through as is.
                        args << (binding.rule instanceof CharSequence ? binding.rule.toString() : binding.rule)
                    }

                    "grails.rabbit.binding.${binding.exchange}.${binding.queue}"(Binding, binding.queue, QUEUE, binding.exchange, binding.rule, binding.arguments )
                }
            }
			
            rabbitRetryHandler(StatefulRetryOperationsInterceptorFactoryBean) {
                def retryPolicy = new SimpleRetryPolicy()
                def maxRetryAttempts = 1
                if(rabbitmqConfig?.retryPolicy?.containsKey('maxAttempts')) {
                    def maxAttemptsConfigValue = rabbitmqConfig.retryPolicy.maxAttempts
                    if(maxAttemptsConfigValue instanceof Integer) {
                        maxRetryAttempts = maxAttemptsConfigValue
                    } else {
                        log.error "rabbitmq.retryPolicy.maxAttempts [$maxAttemptsConfigValue] of type [${maxAttemptsConfigValue.getClass().getName()}] is not an Integer and will be ignored.  The default value of [${maxRetryAttempts}] will be used"
                    }
                }
                retryPolicy.maxAttempts = maxRetryAttempts
                
                def backOffPolicy = new FixedBackOffPolicy()
                backOffPolicy.backOffPeriod = 5000
                
                def retryTemplate = new RetryTemplate()
                retryTemplate.retryPolicy  = retryPolicy
                retryTemplate.backOffPolicy = backOffPolicy
                
                retryOperations = retryTemplate
            }
        }   
    }
    
    def doWithDynamicMethods = { appCtx ->
        addDynamicMessageSendingMethods application.allClasses, appCtx
    }
    
    private addDynamicMessageSendingMethods(classes, ctx) {
        if(ctx.rabbitMQConnectionFactory) {
            classes.each { clz ->
                RabbitDynamicMethods.applyAllMethods(clz, ctx)
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        def containerBeans = applicationContext.getBeansOfType(SimpleMessageListenerContainer)
        if(applicationContext.rabbitTemplate.messageConverter instanceof org.springframework.amqp.support.converter.AbstractMessageConverter) {
            applicationContext.rabbitTemplate.messageConverter.createMessageIds = true
        }
        containerBeans.each { beanName, bean ->
            if(isServiceListener(beanName)) {
                initialiseAdviceChain bean, applicationContext

                // Now that the listener is properly configured, we can start it.
                bean.start()
            }
        }
    }
    
    def onChange = { evt ->
        if(evt.source instanceof Class) {
            addDynamicMessageSendingMethods ([evt.source], evt.ctx)
            
            // If a service has changed, reload the associated beans
            if(isServiceEventSource(application, evt.source)) {
                def serviceGrailsClass = application.addArtefact(ServiceArtefactHandler.TYPE, evt.source)
                def serviceConfigurer = new RabbitServiceConfigurer(
                        serviceGrailsClass,
                        application.config.rabbitmq)
                if (serviceConfigurer.isListener()) {
                    def beans = beans {
                        serviceConfigurer.configure(delegate)
                    }
                    beans.registerBeans(evt.ctx)
                    startServiceListener(serviceGrailsClass.propertyName, evt.ctx)
                }
            } 

            // Other listener containers may have been stopped if they were
            // affected by the re-registering of the changed class. For example,
            // if the Rabbitmq consumer service directly or indirectly depends
            // on a modified service. So we need to restart those that aren't
            // running.
            def containerBeans = applicationContext.getBeansOfType(SimpleMessageListenerContainer)
            containerBeans.each { beanName, bean ->
                if (!bean.running) {
                    initialiseAdviceChain bean, applicationContext
                    bean.start()
                }
            }
        }
    }

    protected isServiceListener(beanName) {
        return beanName.endsWith(RabbitServiceConfigurer.LISTENER_CONTAINER_SUFFIX)
    }

    protected isServiceEventSource(application, source) {
        return application.isArtefactOfType(ServiceArtefactHandler.TYPE, source)
    }

    protected startServiceListener(servicePropertyName, applicationContext) {
        def beanName = servicePropertyName + RabbitServiceConfigurer.LISTENER_CONTAINER_SUFFIX
        applicationContext.getBean(beanName).start()
    }

    protected initialiseAdviceChain(listenerBean, applicationContext) {
        def retryTemplate = applicationContext.rabbitRetryHandler.retryOperations
        def cache = new MapRetryContextCache()
        retryTemplate.retryContextCache = cache

        def missingIdAdvice = new MissingMessageIdAdvice(cache)
        listenerBean.adviceChain = [missingIdAdvice, applicationContext.rabbitRetryHandler] as Advice[]
    }
}
