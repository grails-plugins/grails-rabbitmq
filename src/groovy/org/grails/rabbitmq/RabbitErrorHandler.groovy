package org.grails.rabbitmq

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.ErrorHandler

/**
 * A simple error handler that logs exceptions via SLF4J.
 */
class RabbitErrorHandler implements ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(this)

    void handleError(Throwable t) {
        log.error "Rabbit service listener failed.", t
    }
}
