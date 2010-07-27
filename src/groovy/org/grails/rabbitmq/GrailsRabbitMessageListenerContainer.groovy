package org.grails.rabbitmq
    
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

class GrailsRabbitMessageListenerContainer extends SimpleMessageListenerContainer {
    
    void afterPropertiesSet() {
        def amqpAdmin = new RabbitAdmin(connectionFactory)
        amqpAdmin.declareQueue(new Queue(queueName))
        super.afterPropertiesSet()
    }
}