package org.grails.rabbitmq

import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.SmartLifecycle

class GrailsRabbitAdmin extends RabbitAdmin implements SmartLifecycle, ApplicationContextAware {

    ApplicationContext applicationContext
    private volatile boolean running

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
        def queues = applicationContext.getBeansOfType(Queue)?.values()
        queues?.each { queue ->
            declareQueue(queue)
        }
        running = true
    }

    void stop() {
        running = false
    }

    boolean isRunning() {
        running
    }
}