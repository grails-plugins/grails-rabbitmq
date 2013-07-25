package org.grails.rabbitmq

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA

import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.AmqpException

class MessageBuilder {
    /**
     * Rabbit Template bean.
     */
    RabbitTemplate rabbitTemplate = null

    /**
     * Grails application context.
     */
    DefaultGrailsApplication grailsApplication = null

    /**
     * Routing key to send the message to.
     */
    String routingKey = null

    /**
     * Exchange to send the message to.
     */
    String exchange = null

    /**
     * RPC timeout, in milliseconds.
     */
    long timeout = RabbitTemplate.DEFAULT_REPLY_TIMEOUT

    /**
     * Message body.
     */
    Object message

    /**
     * Message headers.
     */
    Map headers = [:]

    /**
     * Correlation id.
     */
    String correlationId

    /**
     * Priority.
     */
    int priority

    /**
     * Whether to auto-convert the reply payload.
     */
    boolean autoConvert = true

    /**
     * Constructor
     *
     * Loads the rabbit template bean registered from the grails plugin.
     */
    public MessageBuilder() {
        // Grab the application context
        GrailsWebApplicationContext context = SCH.servletContext.getAttribute(GA.APPLICATION_CONTEXT)

        // Grab the rabbit template
        rabbitTemplate = context.getBean('rabbitTemplate')

        // Grab the grails application context
        grailsApplication = context.grailsApplication
    }

    /**
     * Sends a message to the rabbit service.
     */
    protected void doSend() {
        // Get properties
        MessageProperties properties = getProperties()

        // Convert the object and create the message
        Message prepared = createMessage(message, properties)

        // Send the message
        if (exchange) {
            rabbitTemplate.send(exchange, routingKey, prepared)
        }
        else {
            rabbitTemplate.send(routingKey, prepared)
        }
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @throws IllegalArgumentException
     *
     * <p>
     * Thrown under two conditions:
     * <br /><br />
     * 1. If the <code>exchange</code> and <code>routingKey</code> are both <code>null</code>. At least one must be provided.
     * <br /><br />
     * 2. If the <code>message</code> is <code>null</code>.
     * </p>
     */
    public void send() {
        if (! (exchange || routingKey)) {
            throw new IllegalArgumentException("Exchange and/or Routing Key required.")
        }

        if (!message) {
            throw new IllegalArgumentException("Message required")
        }

        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param closure
     */
    public void send(Closure closure) {
        // Run the closure
        run closure

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param routingKey Routing key to send the message to.
     * @param message Message payload.
     */
    public void send(String routingKey, Object message) {
        // Set the params
        this.routingKey = routingKey
        this.message = message

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the rabbit service.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param message Message payload.
     */
    public void send(String exchange, String routingKey, Object message) {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.message = message

        // Send the message
        doSend()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @return
     */
    protected Object doRpc() {
        // Get properties
        MessageProperties properties = getProperties()

        // Convert the object and create the message
        Message prepared = createMessage(message, properties)

        // Set the timeout
        rabbitTemplate.setReplyTimeout(timeout)

        // Send the message
        Message result
        try {
            if (exchange) {
                result = rabbitTemplate.sendAndReceive(exchange, routingKey, prepared)
            }
            else {
                result = rabbitTemplate.sendAndReceive(routingKey, prepared)
            }
        }
        catch (AmqpException e) {
            throw new CouldNotConnectException()
        }

        // Reset the timeout
        rabbitTemplate.setReplyTimeout(RabbitTemplate.DEFAULT_REPLY_TIMEOUT)

        // Check for no result
        if (result == null) {
            throw new NoResponseException()
        }

        // Check for auto conversion
        if (autoConvert) {
            return convertReply(result)
        }

        return result
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * @return
     *
     * @throws IllegalArgumentException
     *
     * <p>
     * Thrown under two conditions:
     * <br /><br />
     * 1. If the <code>exchange</code> and <code>routingKey</code> are both <code>null</code>. At least one must be provided.
     * <br /><br />
     * 2. If the <code>message</code> is <code>null</code>.
     * </p>
     */
    public Object rpc() {
        if (! (exchange || routingKey)) {
            throw new IllegalArgumentException("Exchange and/or Routing Key required.")
        }

        if (!message) {
            throw new IllegalArgumentException("Message required")
        }

        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param closure
     * @return
     */
    public Object rpc(Closure closure) {
        // Run the closure
        run closure

        // Send the message
        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param routingKey Routing key to send the message to.
     * @param message Message payload.
     */
    public Object rpc(String routingKey, Object message) {
        // Set the params
        this.routingKey = routingKey
        this.message = message

        // Send the message
        return doRpc()
    }

    /**
     * Sends a message to the bus and waits for a reply, up to the "timeout" property.
     *
     * This method returns a Message object if autoConvert is set to false, or some
     * other object type (string, list, map) if autoConvert is true.
     *
     * @param exchange Exchange to send the message to.
     * @param routingKey Routing key to send the message to.
     * @param message Message payload.
     */
    public Object rpc(String exchange, String routingKey, Object message) {
        // Set the params
        this.exchange = exchange
        this.routingKey = routingKey
        this.message = message

        // Send the message
        doRpc()
    }

    /**
     * Attempts to convert the reply message payload to a string or list/map.
     *
     * @param message Message to convert
     * @return Converted message
     */
    protected Object convertReply(Message message) {
        // Get the content type
        String contentType = message.getMessageProperties().getContentType()

        // If the content type is binary, just return the byte array
        if (contentType.equalsIgnoreCase('application/octet-stream')) {
            return message.getBody()
        }

        // Convert the result to a string
        String string = new String(message.getBody())

        // Attempt to convert to JSON
        try {
            return new JsonSlurper().parseText(string)
        }
        catch (Exception e) {
            // NOOP
        }

        // Just return the converted string
        return string
    }

    /**
     * Creates the message properties.
     *
     */
    protected MessageProperties getProperties() {
        // Create message properties
        def properties = new MessageProperties()

        // Set any headers
        headers.each { key, value ->
            properties.setHeader(key, value)
        }

        // Set correlation id
        if (correlationId) {
            properties.setCorrelationId(correlationId.getBytes())
        }

        // Set priority
        if (priority) {
            properties.setPriority(priority)
        }

        return properties
    }

    /**
     * Converts the payload object and creates the message object.
     *
     * @param source Object to convert.
     * @return Source object converted to a byte array.
     */
    protected Message createMessage(Object source, MessageProperties properties) {
        // Check for lists or maps
        if (source instanceof List || source instanceof Map) {
            source = new JSON(source)
        }

        // Check for domains
        if (grailsApplication.isDomainClass(source.getClass())) {
            source = new JSON(source)
        }

        // Check for JSON
        if (source instanceof JSON) {
            properties.setContentType('application/json')
            return new Message(source.toString().getBytes(), properties)
        }

        // Check for GStrings
        if (source instanceof GString) {
            source = source.toString()
        }

        // Do automatic conversion (this doesn't always work)
        return rabbitTemplate.getMessageConverter().toMessage(source, properties)
    }

    /**
     * Runs a passed closure to implement builder-style operation.
     *
     * @param closure
     */
    protected void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.OWNER_FIRST
        closure.run()
    }
}

/**
 * Thrown when there is no response to an RPC message.
 */
class NoResponseException extends Exception { }

/**
 * Thrown when there was a problem connecting to the rabbitmq service.
 */
class CouldNotConnectException extends Exception { }
