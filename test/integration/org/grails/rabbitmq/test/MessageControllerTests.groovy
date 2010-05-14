package org.grails.rabbitmq.test

import grails.test.ControllerUnitTestCase
import groovy.mock.interceptor.MockFor
import org.springframework.amqp.rabbit.core.RabbitTemplate


class MessageControllerTests extends ControllerUnitTestCase {
    
    void testRabbitSendCallsConvertAndSend() {
        def queueName
        def messageSent
        
        mockParams.msg = 'Hello World'
        
        def mockTemplate = new MockFor(RabbitTemplate)
        mockTemplate.demand.convertAndSend { String queue, String message ->
            queueName = queue
            messageSent = message
        }
        mockTemplate.use {
            controller.sendMessage()
        }
        
        assertEquals 'Message: Hello World', messageSent
        assertEquals 'foo', queueName
    }
}