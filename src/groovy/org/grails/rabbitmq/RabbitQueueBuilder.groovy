package org.grails.rabbitmq

import org.springframework.amqp.core.Queue

class RabbitQueueBuilder {
    
    def queues = []
    
    def methodMissing(String methodName, args) {
        def queue = new Queue(methodName)
        queues << queue
    }
}