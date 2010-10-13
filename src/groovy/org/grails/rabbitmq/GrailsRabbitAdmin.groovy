package org.grails.rabbitmq

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.SmartLifecycle

class GrailsRabbitAdmin extends RabbitAdmin implements SmartLifecycle, ApplicationContextAware {

    ApplicationContext applicationContext
    private boolean running

    GrailsRabbitAdmin(ConnectionFactory connectionFactory) {
        super(connectionFactory)
    }

    boolean isAutoStartup() {
        true
    }

    void stop(Runnable callback) {
        callback.run()
        running = false
    }

    int getPhase() {
        Integer.MIN_VALUE
    }

    void start() {
        def exchanges = applicationContext.getBeansOfType(Exchange)?.values()
        exchanges?.each { ex -> declareExchange ex }
        
        def queues = applicationContext.getBeansOfType(Queue)?.values()
        queues?.each { queue -> declareQueue queue }
        
        def bindings = applicationContext.getBeansOfType(Binding)?.values()
        bindings?.each { b -> declareBinding b }
        
        running = true
    }

    void stop() {
        running = false
    }

    boolean isRunning() {
        running
    }
}