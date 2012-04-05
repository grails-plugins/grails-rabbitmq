package org.grails.rabbitmq

import org.springframework.amqp.core.*
import org.slf4j.LoggerFactory

class RabbitQueueBuilder {

    private final static log = LoggerFactory.getLogger(RabbitQueueBuilder)
    
    def bindings = []
    def exchanges = []
    def queues = []

    private currentExchange
    
    def methodMissing(String methodName, args) {
        def queue

        def argsMap = args ? args[0] : [:]
        if(argsMap) {
            def autoDelete = Boolean.valueOf(argsMap.autoDelete)
            def exclusive = Boolean.valueOf(argsMap.exclusive)
            def durable = Boolean.valueOf(argsMap.durable)
            def arguments
            if(argsMap.arguments instanceof Map) {
                arguments = argsMap.arguments
            }
            queue = new Queue(methodName, durable, exclusive, autoDelete, arguments)
        } else {
            queue = new Queue(methodName)
        }

        // If we are nested inside of an exchange definition, create
        // a binding between the queue and the exchange.
        if (currentExchange) {
            def newBinding = [ queue: methodName, exchange: currentExchange.name ]
            bindings << newBinding

            switch (currentExchange.type) {
            case DirectExchange:
                if (argsMap.binding && !(argsMap.binding instanceof CharSequence)) {
                    throw new RuntimeException(
                            "The binding for queue '${methodName}' to direct " +
                            "exchange '${currentExchange.name}' must be a string.")
                }

                // Use the name of the queue as a default binding if no
                // explicit one is declared.
                newBinding.rule = argsMap.binding ?: queue.name
                break

            case FanoutExchange:
                // Any binding will be ignored.
                log.warn "'${currentExchange.name}' is a fanout exchange - binding for queue '${methodName}' ignored"
                newBinding.rule = ""    // rabbit client API doesn't like a null binding
                break

            case HeadersExchange:
                if (!(argsMap.binding instanceof Map)) {
                    throw new RuntimeException(
                            "The binding for queue '${methodName}' to headers " +
                            "exchange '${currentExchange.name}' must be declared " +
                            "and must be a map.")
                }

                newBinding.rule = argsMap.binding
                break

            case TopicExchange:
                if (!(argsMap.binding instanceof CharSequence)) {
                    throw new RuntimeException(
                            "The binding for queue '${methodName}' to topic " +
                            "exchange '${currentExchange.name}' must be declared " +
                            "and must be a string.")
                }

                newBinding.rule = argsMap.binding
                break
            }
        }
        queues << queue
    }

    /**
     * Defines a new exchange.
     * @param args The properties of the exchange, such as its name
     * and whether it is durable or not.
     * @param c An optional closure that includes queue definitions.
     * If provided, the queues are bound to this exchange.
     */
    def exchange(Map args, Closure c = null) {
        if (currentExchange) throw new RuntimeException("Cannot declare an exchange within another exchange")
        if (!args.name) throw new RuntimeException("A name must be provided for the exchange")
        if (!args.type) throw new RuntimeException("A type must be provided for the exchange '${args.name}'")
        exchanges << [ *:args ]

        if (c) {
            currentExchange = exchanges[-1]
            c = c.clone()
            c.delegate = this
            c()
        }

        // Clear the current exchange regardless of whether there was
        // a closure argument or not. Just an extra safety measure.
        currentExchange = null
    }

    def getDirect() { return DirectExchange }
    def getFanout() { return FanoutExchange }
    def getHeaders() { return HeadersExchange }
    def getTopic() { return TopicExchange }
}
