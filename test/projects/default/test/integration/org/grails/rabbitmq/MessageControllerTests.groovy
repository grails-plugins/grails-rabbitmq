package org.grails.rabbitmq

import grails.test.ControllerUnitTestCase
import groovy.mock.interceptor.MockFor
import org.grails.rabbitmq.test.MessageController
import org.springframework.amqp.rabbit.core.RabbitTemplate

class MessageControllerTests extends ControllerUnitTestCase {

    MessageControllerTests() {
        super(MessageController)
    }

    void testRabbitSendCallsConvertAndSend() {
        def stringMessageQueueName
        def stringMessage

        def mapMessageQueueName
        def mapMessage

        mockParams.msg = 'Hello World'

        def mockTemplate = new MockFor(RabbitTemplate)
        mockTemplate.demand.convertAndSend { String queue, String message ->
            stringMessageQueueName = queue
            stringMessage = message
        }
        mockTemplate.demand.convertAndSend { String queue, Map message ->
            mapMessage = message
            mapMessageQueueName = queue
        }
        mockTemplate.use {
            controller.sendMessage()
        }

        assertEquals 'Message: Hello World', stringMessage
        assertEquals 'foo', stringMessageQueueName

        assertEquals 'foo', mapMessageQueueName
        assertEquals 2, mapMessage?.size()
        assertEquals 'Hello World', mapMessage.msgBody
        assertTrue mapMessage.msgTime instanceof Date
    }
}