package org.grails.rabbitmq

import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.TopicExchange

class RabbitQueueBuilderTests extends GroovyTestCase {

    void testSimpleQueue() {
        def closure = {
            foo()
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def queues = builder.queues
        assertEquals 'wrong number of queues', 1, queues?.size()
        def queue = queues[0]

        assertEquals 'wrong queue name', 'foo', queue.name
        assertEquals 'wrong durable value', true, queue.durable
        assertEquals 'wrong auto delete value', false, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertNull 'no arguments expected', queue.arguments
    }

    void testQueueConfigurationDetails() {
        def closure = {
            foo autoDelete: true, durable: false, exclusive: true
            bar autoDelete: false, durable: true, exclusive: false
            baz autoDelete: true, arguments: [lang: 'Groovy', framework: 'Grails']
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def queues = builder.queues
        assertEquals 'wrong number of queues', 3, queues?.size()
        def queue = queues[0]

        assertEquals 'wrong queue name', 'foo', queue.name
        assertEquals 'wrong durable value', false, queue.durable
        assertEquals 'wrong auto delete value', true, queue.autoDelete
        assertEquals 'wrong exclusive value', true, queue.exclusive
        assertNull 'no arguments expected', queue.arguments

        queue = queues[1]

        assertEquals 'wrong queue name', 'bar', queue.name
        assertEquals 'wrong durable value', true, queue.durable
        assertEquals 'wrong auto delete value', false, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertNull 'no arguments expected', queue.arguments

        queue = queues[2]

        assertEquals 'wrong queue name', 'baz', queue.name
        assertEquals 'wrong durable value', false, queue.durable
        assertEquals 'wrong auto delete value', true, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertEquals 'wrong number of arguments', 2, queue.arguments?.size()
        assertEquals 'lang argument had wrong value', 'Groovy', queue.arguments.lang
        assertEquals 'framework argument had wrong value', 'Grails', queue.arguments.framework
    }

    void testFanoutExchangeDefinition() {
        def closure = {
            exchange name: 'my.fanout', type: fanout, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'my.fanout', exchanges[0].name
        assertEquals 'wrong exchange type', FanoutExchange, exchanges[0].type
        assertEquals 'exchange is not durable', true, exchanges[0].durable
        assertNull 'exchange has auto-delete set', exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        def queues = builder.queues
        assertEquals 'wrong number of queues', 1, queues?.size()
        assertEquals 'wrong queue name', 'foo', queues[0].name
        assertEquals 'wrong durable value', false, queues[0].durable
        assertEquals 'wrong auto delete value', true, queues[0].autoDelete
        assertEquals 'wrong exclusive value', true, queues[0].exclusive
        assertNull 'no arguments expected', queues[0].arguments

        def bindings = builder.bindings
        assertEquals 'wrong number of bindings', 1, bindings?.size()
        assertEquals 'wrong exchange bound', 'my.fanout', bindings[0].exchange
        assertEquals 'wrong queue bound', 'foo', bindings[0].queue
        assertEquals 'binding is set', '', bindings[0].rule
        assertNull 'no arguments expected', bindings[0].arguments
    }

    void testStandaloneFanoutExchangeDefinition() {
        def closure = {
            exchange name: 'non.durable.fanout', type: fanout, durable: false, autoDelete: true
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'non.durable.fanout', exchanges[0].name
        assertEquals 'wrong exchange type', FanoutExchange, exchanges[0].type
        assertEquals 'exchange is durable', false, exchanges[0].durable
        assertEquals 'exchange does not have auto-delete set', true, exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        assertEquals 'wrong number of queues', 0, builder.queues?.size()
        assertEquals 'wrong number of bindings', 0, builder.bindings?.size()
    }

    void testTopicExchangeDefinition() {
        def closure = {
            exchange name: 'my.topic', type: topic, durable: false, {
                foo durable: true, binding: 'shares.#'
                bar durable: false, autoDelete: true, binding: 'shares.nyse.?'
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'my.topic', exchanges[0].name
        assertEquals 'wrong exchange type', TopicExchange, exchanges[0].type
        assertEquals 'exchange is durable', false, exchanges[0].durable
        assertNull 'exchange has auto-delete set', exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        def queues = builder.queues
        assertEquals 'wrong number of queues', 2, queues?.size()
        assertEquals 'wrong queue name', 'foo', queues[0].name
        assertEquals 'wrong durable value', true, queues[0].durable
        assertEquals 'queue is auto delete', false, queues[0].autoDelete
        assertEquals 'queue is exclusive', false, queues[0].exclusive
        assertNull 'no arguments expected', queues[0].arguments
        assertEquals 'wrong queue name', 'bar', queues[1].name
        assertEquals 'wrong durable value', false, queues[1].durable
        assertEquals 'queue is not auto delete', true, queues[1].autoDelete
        assertEquals 'queue is exclusive', false, queues[1].exclusive
        assertNull 'no arguments expected', queues[1].arguments

        def bindings = builder.bindings
        assertEquals 'wrong number of bindings', 2, bindings?.size()
        assertEquals 'wrong exchange bound', 'my.topic', bindings[0].exchange
        assertEquals 'wrong queue bound', 'foo', bindings[0].queue
        assertEquals 'wrong binding rule', 'shares.#', bindings[0].rule
        assertEquals 'wrong exchange bound', 'my.topic', bindings[1].exchange
        assertEquals 'wrong queue bound', 'bar', bindings[1].queue
        assertEquals 'wrong binding rule', 'shares.nyse.?', bindings[1].rule
        assertNull 'no arguments expected', bindings[0].arguments
    }

    void testStandaloneTopicExchangeDefinition() {
        def closure = {
            exchange name: 'another.topic', type: topic
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'another.topic', exchanges[0].name
        assertEquals 'wrong exchange type', TopicExchange, exchanges[0].type
        assertNull 'durable is set', exchanges[0].durable
        assertNull 'auto delete is set', exchanges[0].autoDelete

        assertEquals 'wrong number of queues', 0, builder.queues?.size()
        assertEquals 'wrong number of bindings', 0, builder.bindings?.size()
    }

    void testTopicExchangeDefinitionWithNoBinding() {
        def closure = {
            exchange name: 'bad.topic', type: topic, {
                foo durable: false, exclusive: true
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder

        def msg = shouldFail(RuntimeException) {
            closure()
        }

        assertTrue 'wrong exception message', msg.contains("queue 'foo'")
        assertTrue 'wrong exception message', msg.contains("exchange 'bad.topic'")
        assertTrue 'wrong exception message', msg.contains("must be declared")
    }

    void testTopicExchangeDefinitionWithInvalidBinding() {
        def closure = {
            exchange name: 'bad.topic', type: topic, {
                foo durable: false, exclusive: true, binding: [ key: 'shares.#' ]
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder

        def msg = shouldFail(RuntimeException) {
            closure()
        }

        assertTrue 'wrong exception message', msg.contains("queue 'foo'")
        assertTrue 'wrong exception message', msg.contains("exchange 'bad.topic'")
        assertTrue 'wrong exception message', msg.contains("must be a string")
    }

    void testDirectExchangeDefinitionWithImplicitBinding() {
        def closure = {
            exchange name: 'my.direct', type: direct, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'my.direct', exchanges[0].name
        assertEquals 'wrong exchange type', DirectExchange, exchanges[0].type
        assertEquals 'exchange is not durable', true, exchanges[0].durable
        assertNull 'exchange has auto-delete set', exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        def queues = builder.queues
        assertEquals 'wrong number of queues', 1, queues?.size()
        assertEquals 'wrong queue name', 'foo', queues[0].name
        assertEquals 'wrong durable value', false, queues[0].durable
        assertEquals 'wrong auto delete value', true, queues[0].autoDelete
        assertEquals 'wrong exclusive value', true, queues[0].exclusive
        assertNull 'no arguments expected', queues[0].arguments

        def bindings = builder.bindings
        assertEquals 'wrong number of bindings', 1, bindings?.size()
        assertEquals 'wrong exchange bound', 'my.direct', bindings[0].exchange
        assertEquals 'wrong queue bound', 'foo', bindings[0].queue
        assertEquals 'binding is wrong', 'foo', bindings[0].rule
        assertNull 'no arguments expected', bindings[0].arguments
    }

    void testDirectExchangeDefinitionWithExplicitBinding() {
        def closure = {
            exchange name: 'my.direct', type: direct, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true, binding: 'shares.nyse.vmw'
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'my.direct', exchanges[0].name
        assertEquals 'wrong exchange type', DirectExchange, exchanges[0].type
        assertEquals 'exchange is not durable', true, exchanges[0].durable
        assertNull 'exchange has auto-delete set', exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        def queues = builder.queues
        assertEquals 'wrong number of queues', 1, queues?.size()
        assertEquals 'wrong queue name', 'foo', queues[0].name
        assertEquals 'wrong durable value', false, queues[0].durable
        assertEquals 'wrong auto delete value', true, queues[0].autoDelete
        assertEquals 'wrong exclusive value', true, queues[0].exclusive
        assertNull 'no arguments expected', queues[0].arguments

        def bindings = builder.bindings
        assertEquals 'wrong number of bindings', 1, bindings?.size()
        assertEquals 'wrong exchange bound', 'my.direct', bindings[0].exchange
        assertEquals 'wrong queue bound', 'foo', bindings[0].queue
        assertEquals 'binding is wrong', 'shares.nyse.vmw', bindings[0].rule
        assertNull 'no arguments expected', bindings[0].arguments
    }

    void testStandaloneDirectExchangeDefinition() {
        def closure = {
            exchange name: 'my.direct', type: direct, durable: false, autoDelete: true
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges
        assertEquals 'wrong number of exchanges', 1, exchanges?.size()
        assertEquals 'wrong exchange name', 'my.direct', exchanges[0].name
        assertEquals 'wrong exchange type', DirectExchange, exchanges[0].type
        assertEquals 'exchange is durable', false, exchanges[0].durable
        assertEquals 'exchange does not have auto-delete set', true, exchanges[0].autoDelete
        assertNull 'no arguments expected', exchanges[0].arguments

        assertEquals 'wrong number of queues', 0, builder.queues?.size()
        assertEquals 'wrong number of bindings', 0, builder.bindings?.size()
    }

    void testDirectExchangeDefinitionWithInvalidBinding() {
        def closure = {
            exchange name: 'bad.direct', type: direct, {
                foo durable: false, exclusive: true, binding: [ key: 'shares.#' ]
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder

        def msg = shouldFail(RuntimeException) {
            closure()
        }

        assertTrue 'wrong exception message', msg.contains("queue 'foo'")
        assertTrue 'wrong exception message', msg.contains("exchange 'bad.direct'")
        assertTrue 'wrong exception message', msg.contains("must be a string")
    }
}
