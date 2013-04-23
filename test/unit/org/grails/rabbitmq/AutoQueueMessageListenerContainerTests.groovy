package org.grails.rabbitmq

import com.rabbitmq.client.AMQP.Queue.DeclareOk
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Consumer
import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.Connection
import org.springframework.amqp.rabbit.connection.ConnectionFactory

class AutoQueueMessageListenerContainerTests extends GrailsUnitTestCase {
    def mockContext = new MockApplicationContext()
    def mockAdminBean = new Expando()
    def testContainer = new AutoQueueMessageListenerContainer()

    void setUp() {
        mockContext.registerMockBean("adm", mockAdminBean)

        testContainer.applicationContext = mockContext
        testContainer.connectionFactory = [
            createConnection: {-> [
                createChannel: { boolean transactional -> [
                    basicQos: {int qos -> },
                    queueDeclarePassive: {String s -> [:] as DeclareOk },
                    basicConsume: {String s, boolean b, Consumer c -> "Test" }
                ] as Channel },
                close: {-> /* Do nothing */ }
            ] as Connection }
        ] as ConnectionFactory
    }

    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * topic exchange with the given name.
     */
    void testDoStartWithTopicExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.topic"

        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.destination == tempQueueName
            assert binding.routingKey == '#'
            declareBindingCalled = true
        }

        mockContext.registerMockBean(exchangeName, new TopicExchange(exchangeName))

        testContainer.exchangeBeanName = exchangeName
        testContainer.doStart()

        assertTrue "declareBinding() not called", declareBindingCalled
    }

    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * topic exchange with the given name and the given routing key.
     */
    void testDoStartWithTopicExchangeAndRoutingKey() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1235"
        def exchangeName = "another.topic"
        def routingKey = "my.routing.#"

        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.destination == tempQueueName
            assert binding.routingKey == routingKey
            declareBindingCalled = true
        }

        mockContext.registerMockBean(exchangeName, new TopicExchange(exchangeName))

        testContainer.exchangeBeanName = exchangeName
        testContainer.routingKey = routingKey
        testContainer.doStart()

        assertTrue "declareBinding() not called", declareBindingCalled
    }

    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * fanout exchange with the given name. The routing key should not be set.
     */
    void testDoStartWithFanoutExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.fanout"

        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.destination == tempQueueName
            assert !binding.routingKey
            declareBindingCalled = true
        }

        mockContext.registerMockBean(exchangeName, new FanoutExchange(exchangeName))

        testContainer.exchangeBeanName = exchangeName
        testContainer.doStart()

        assertTrue "declareBinding() not called", declareBindingCalled
    }

    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * fanout exchange with the given name. Even if a routing key is given, it
     * should be ignored.
     */
    void testDoStartWithFanoutExchangeAndRoutingKey() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1235"
        def exchangeName = "another.fanout"
        def routingKey = "my.routing.#"

        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.destination == tempQueueName
            assert !binding.routingKey
            declareBindingCalled = true
        }

        mockContext.registerMockBean(exchangeName, new FanoutExchange(exchangeName))

        testContainer.exchangeBeanName = exchangeName
        testContainer.routingKey = routingKey
        testContainer.doStart()

        assertTrue "declareBinding() not called", declareBindingCalled
    }

    /**
     * No binding should be declared if the exchange is not a fanout or topic.
     */
    void testDoStartWithDirectExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.direct"

        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.destination == tempQueueName
            declareBindingCalled = true
        }

        mockContext.registerMockBean(exchangeName, new DirectExchange(exchangeName))

        testContainer.exchangeBeanName = exchangeName
        testContainer.doStart()

        assertTrue "declareBinding() was not called", declareBindingCalled
    }
}
