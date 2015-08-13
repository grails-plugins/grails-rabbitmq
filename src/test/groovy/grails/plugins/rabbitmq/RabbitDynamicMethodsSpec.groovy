package grails.plugins.rabbitmq

import org.grails.support.MockApplicationContext
import org.springframework.amqp.core.AmqpTemplate
import spock.lang.Specification

/**
 * Test case for {@link RabbitDynamicMethods}.
 */
class RabbitDynamicMethodsSpec extends Specification {
    void setupSpec() {
//        super.setup()

        // Ensures that the dynamic methods we add to Map are removed
        // once each test is complete.
        Mock(Map)
    }

    void testRabbitRpcSend() {
        setup:
        AmqpTemplate tmplControl = Mock()

        when:
        def testContext = new MockApplicationContext()
        testContext.registerMockBean "rabbitTemplate", tmplControl
        RabbitDynamicMethods.applyRabbitRpcSend(Map, testContext)

        def testObj = [:]
        testObj.rabbitRpcSend "ex", "myQ", "Some message", "replyQ"

        then:
        // Check that the convertAndSend() method was invoked.
        1 * tmplControl.convertAndSend(_)
    }
}
