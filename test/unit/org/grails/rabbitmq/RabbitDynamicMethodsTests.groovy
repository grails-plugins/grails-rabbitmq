package org.grails.rabbitmq

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.MessageProperties

import grails.test.GrailsUnitTestCase

/**
 * Test case for {@link RabbitDynamicMethods}.
 */
class RabbitDynamicMethodsTests extends GrailsUnitTestCase {
    void setUp() {
        super.setUp()

        // Ensures that the dynamic methods we add to Map are removed
        // once each test is complete.
        mockFor(Map)
    }

    void testRabbitRpcSend() {
        def tmplControl = mockFor(AmqpTemplate)
        tmplControl.demand.convertAndSend { String ex, String q, Object content, MessagePostProcessor mpp ->
            assert ex == "ex"
            assert q == "myQ"
            assert content == "Some message"

            // Execute the callback and make sure that it sets reply queue
            // in the message properties properly.
            def msg = new Message(ex.bytes, new MessageProperties())
            mpp.postProcessMessage(msg)
            assert msg.messageProperties?.replyToAddress?.routingKey == "replyQ"
        }

        def testContext = new MockApplicationContext()
        testContext.registerMockBean "rabbitTemplate", tmplControl.createMock()
        RabbitDynamicMethods.applyRabbitRpcSend(Map, testContext)

        def testObj = [:]
        testObj.rabbitRpcSend "ex", "myQ", "Some message", "replyQ"

        // Check that the convertAndSend() method was invoked.
        tmplControl.verify()
    }
}
