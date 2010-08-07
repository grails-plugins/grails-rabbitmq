package org.grails.rabbitmq

import org.springframework.amqp.core.Queue

class RabbitQueueBuilder {
    
    def queues = []
    
    def methodMissing(String methodName, args) {
        def queue = new Queue(methodName)

        if(args) {
            def argsMap = args[0]
            queue.autoDelete = Boolean.valueOf(argsMap.autoDelete)
            queue.exclusive = Boolean.valueOf(argsMap.exclusive)
            queue.durable = Boolean.valueOf(argsMap.durable)
            if(argsMap.arguments instanceof Map) {
                queue.arguments = argsMap.arguments
            }
        }
        queues << queue
    }
}