/*
 * Copyright 2012 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.rabbitmq

import org.slf4j.LoggerFactory

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.grails.rabbitmq.AutoQueueMessageListenerContainer
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.beans.InvalidPropertyException

/**
 * <p>Configures the Spring beans required to set up a service as an AMQP
 * listener. The service must have either a static <tt>rabbitQueue</tt>
 * property declared or a static <tt>rabbitSubscribe</tt> one (but not
 * both). If neither of these exist, nothing is configured. Otherwise,
 * it will configure a {@link SimpleMessageListenerContainer} and an
 * associated {@link MessageListenerAdapter}.</p>
 */
class RabbitServiceConfigurer {

    private final static log = LoggerFactory.getLogger(RabbitServiceConfigurer)

    public static final String LISTENER_CONTAINER_SUFFIX = "_MessageListenerContainer"
    public static final String MESSAGE_CONVERTER_OPTION = "messageConverterBean"

    private static final String QUEUE = "rabbitQueue"
    private static final String SUBSCRIBE = "rabbitSubscribe"
    private static final Set ADAPTER_OPTIONS = [
            MESSAGE_CONVERTER_OPTION,
            "defaultListenerMethod",
            "encoding",
            "immediatePublish",
            "mandatoryPublish",
            "responseExchange",
            "responseRoutingKey" ]

    private serviceGrailsClass
    private rabbitConfig
    private type
    private options = [:]
    private beanBuilder

    /**
     * Fairly cheap configurer construction. Create new ones as needed. If
     * the given service isn't declared as an AMPQ/Rabbit listener, then
     * the object is successfully created, but {@link #isListener()} will
     * return <tt>false</tt>.
     * @param serviceClass The <tt>GrailsServiceClass</tt> for the service
     * that requires configuration.
     * @param rabbitConfig The Rabbit config, i.e. everthing below 'rabbitmq'
     * in the project's Config.groovy.
     */
    RabbitServiceConfigurer(serviceClass, rabbitConfig) {
        this.serviceGrailsClass = serviceClass
        this.rabbitConfig = rabbitConfig

        validateAndInit()
    }

    /**
     * Configures the Spring beans required to make the associated Grails
     * service an AMQP listener.
     * @param beanBuilder A BeanBuilder instance through which the required
     * Spring beans will be declared.
     */
    def configure(beanBuilder) {
        if (!isListener()) return

        this.beanBuilder = beanBuilder

        def optionGroups = options.groupBy { (it.key in ADAPTER_OPTIONS) ? "adapter" : "container" }
        configureListener(optionGroups["container"], optionGroups["adapter"])
    }

    /**
     * Returns <tt>true</tt> if the associated service is declared as an
     * AMQP listener, otherwise <tt>false</tt>.
     */
    boolean isListener() {
        return type != null
    }

    protected validateAndInit(service) {
        def rabbitQueue = GCU.getStaticPropertyValue(serviceGrailsClass.clazz, QUEUE)
        def rabbitSubscribe = GCU.getStaticPropertyValue(serviceGrailsClass.clazz, SUBSCRIBE)

        if (rabbitQueue && rabbitSubscribe) {
            throw new IllegalStateException(
                    "${serviceGrailsClass.shortName} declares both 'rabbitQueue' and 'rabbitSubscribe'. " +
                    "You can only have one of them in a class.")
        }

        if (rabbitQueue) initForQueue(rabbitQueue)
        else if (rabbitSubscribe) initForSubscribe(rabbitSubscribe)
    }

    protected initForQueue(propertyValue) {
        type = QUEUE
        initOptionsForQueue(propertyValue)
    }

    protected initOptionsForQueue(String queue) {
        if (!queue) {
            throw new IllegalArgumentException(
                    "${serviceGrailsClass.shortName} must declare a non-empty string for its " +
                    "'rabbitQueue' property.")
        }

        initOptionsForQueue([queues: queue])
    }

    protected initOptionsForQueue(Map queueOptions) {
        if (queueOptions.queues == null) {
            throw new IllegalArgumentException(
                    "${serviceGrailsClass.shortName} must declare a 'queues' entry in its " +
                    "'rabbitQueue' map. This must be a queue name or a collection/array of queue names.")
        }

        options << queueOptions

        def queueNames = options.remove("queues")
        if (queueNames instanceof CharSequence) options.queueNames = queueNames.toString()
        else options.queueNames = queueNames as String[]
    }

    protected initForSubscribe(propertyValue) {
        type = SUBSCRIBE
        initOptionsForSubscribe(propertyValue)
    }

    protected initOptionsForSubscribe(String exchange) {
        if (!exchange) {
            throw new IllegalArgumentException(
                    "${serviceGrailsClass.shortName} must declare a non-empty string for its " +
                    "'rabbitSubscribe' property.")
        }

        initOptionsForSubscribe([name: exchange])
    }

    protected initOptionsForSubscribe(Map subscribeOptions) {
        if (!subscribeOptions.name) {
            throw new IllegalArgumentException(
                    "${serviceGrailsClass.shortName} must declare a 'name' entry in its " +
                    "'rabbitSubscribe' map. This must be the name of a valid exchange.")
        }

        options << subscribeOptions
        options.exchangeBeanName = "grails.rabbit.exchange.${options.remove("name")}"
    }

    protected configureListener(containerOptions, adapterOptions) {
        def configHolder = new RabbitConfigurationHolder(rabbitConfig)
        def propertyName = serviceGrailsClass.propertyName
        def serviceConcurrentConsumers = configHolder.getServiceConcurrentConsumers(serviceGrailsClass)
        log.info "Setting up rabbitmq listener for ${serviceGrailsClass.clazz} with " +
                "${serviceConcurrentConsumers} consumer(s)"

        containerOptions = containerOptions ?: [:]
        adapterOptions = adapterOptions ?: [:]

        processMessageConverterEntry(adapterOptions)

        // First add an adapter for the given service that exposes it as a listener.
        beanBuilder."${propertyName}RabbitAdapter"(MessageListenerAdapter) {
            delegate.delegate = ref(propertyName)

            for (entry in adapterOptions) {
                delegate."${entry.key}" = entry.value
            }
        }

        // Next, define the listener container, i.e. the object that receives the
        // messages and passes them on to the listener.
        def listenerClass = type == SUBSCRIBE ? AutoQueueMessageListenerContainer : SimpleMessageListenerContainer
        beanBuilder."${propertyName}${LISTENER_CONTAINER_SUFFIX}"(listenerClass) {
            // There seems to be a bug in Spring AMQP 1.0.0 that treats the default
            // AUTO acknowledge mode as if it's no auto-acking. So we use NONE. If
            // the bug is fixed, this workaround may break things!
            acknowledgeMode = isServiceTransactional() ? AcknowledgeMode.AUTO : AcknowledgeMode.NONE

            // Exceptions from listeners don't seem to be logged by Spring AMQP -
            // not very useful! This simple error handler just performs the logging.
            errorHandler = ref("rabbitErrorHandler")

            // We manually start the listener later, once we know the service is
            // ready to process messages.
            autoStartup = false
            channelTransacted = isServiceTransactional()
            connectionFactory = ref("rabbitMQConnectionFactory")
            concurrentConsumers = serviceConcurrentConsumers
            messageListener = ref("${propertyName}RabbitAdapter")
            for (entry in containerOptions) {
                delegate."${entry.key}" = entry.value
            }
        }
    }

    protected processMessageConverterEntry(adapterOptions) {
        def messageConverterBean = rabbitConfig.messageConverterBean

        if (adapterOptions.containsKey(MESSAGE_CONVERTER_OPTION)) {
            def optionValue = adapterOptions.remove(MESSAGE_CONVERTER_OPTION)
            adapterOptions.messageConverter = optionValue ? beanBuilder.ref(optionValue) : null
        }
        else if (messageConverterBean) {
            adapterOptions.messageConverter = beanBuilder.ref(messageConverterBean)
        }
    }

    protected boolean isServiceTransactional() {
        def propertyName = serviceGrailsClass.propertyName
        def txn = serviceGrailsClass.transactional
        if (!(rabbitConfig."${propertyName}".transactional instanceof ConfigObject)) {
            txn = rabbitConfig."${propertyName}".transactional as Boolean
        }

        return txn
    }
}
