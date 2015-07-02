package grails.plugins.rabbitmq

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.TopicExchange
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class RabbitQueueBuilderSpec extends Specification {

    void testSimpleQueue() {
        when:
        def closure = {
            foo()
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def queues = builder.queues
        then: 'wrong number of queues'
        1 == queues?.size()

        when:
        def queue = queues[0]

        then:
        'foo' == queue.name // wrong queue name
        true == queue.durable // wrong durable value
        false == queue.autoDelete // wrong auto delete value
        false == queue.exclusive // wrong exclusive value
        null == queue.arguments // no arguments expected
    }

    void testQueueConfigurationDetails() {
        when:
        def closure = {
            foo autoDelete: true, durable: false, exclusive: true
            bar autoDelete: false, durable: true, exclusive: false
            baz autoDelete: true, arguments: [lang: 'Groovy', framework: 'Grails']
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def queues = builder.queues

        then: 'wrong number of queues'
        3 == queues?.size()

        when:
        def queue = queues[0]

        then:
        'foo' == queue.name // wrong queue name
        false == queue.durable // wrong durable value
        true == queue.autoDelete // wrong auto delete value
        true == queue.exclusive // wrong exclusive value
        null == queue.arguments // no arguments expected

        when:
        queue = queues[1]

        then:
        'bar' == queue.name // wrong queue name
        true == queue.durable // wrong durable value
        false == queue.autoDelete // wrong auto delete value
        false == queue.exclusive // wrong exclusive value
        null == queue.arguments // no arguments expected

        when:
        queue = queues[2]

        then:
        'baz' == queue.name // wrong queue name
        false == queue.durable // wrong durable value
        true == queue.autoDelete // wrong auto delete value
        false == queue.exclusive // wrong exclusive value
        2 == queue.arguments?.size() // wrong number of arguments
        'Groovy' == queue.arguments.lang // lang argument had wrong value
        'Grails' == queue.arguments.framework // framework argument had wrong value
    }

    void testFanoutExchangeDefinition() {
        when:
        def closure = {
            exchange name: 'my.fanout', type: fanout, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true
            }
        }
        def builder = new RabbitQueueBuilder()
        closure.delegate = builder
        closure()
        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() // wrong number of exchanges
        'my.fanout' == exchanges[0].name // wrong exchange name
        FanoutExchange == exchanges[0].type // wrong exchange type
        true == exchanges[0].durable // exchange is not durable
        null == exchanges[0].autoDelete // exchange has auto-delete set
        null == exchanges[0].arguments // no arguments expected

        when:
        def queues = builder.queues
        then:
        1 == queues?.size() // wrong number of queues
        'foo' == queues[0].name // wrong queue name
        false == queues[0].durable // wrong durable value
        true == queues[0].autoDelete // wrong auto delete value
        true == queues[0].exclusive // wrong exclusive value
        null == queues[0].arguments // no arguments expected

        when:
        def bindings = builder.bindings
        then:
        1 == bindings?.size()  // wrong number of bindings
        'my.fanout' == bindings[0].exchange // wrong exchange bound
        'foo' == bindings[0].queue // wrong queue bound
        '' == bindings[0].rule // binding is set
        null == bindings[0].arguments // no arguments expected
    }

    void testStandaloneFanoutExchangeDefinition() {
        when:
        def closure = {
            exchange name: 'non.durable.fanout', type: fanout, durable: false, autoDelete: true
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() //wrong number of exchanges
        'non.durable.fanout' == exchanges[0].name // wrong exchange name
        FanoutExchange == exchanges[0].type // wrong exchange type
        false == exchanges[0].durable // exchange is durable
        true == exchanges[0].autoDelete // exchange does not have auto-delete set
        null == exchanges[0].arguments // no arguments expected
        0 == builder.queues?.size() // wrong number of queues
        0 == builder.bindings?.size() // wrong number of bindings
    }

    void testTopicExchangeDefinition() {
        when:
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

        then:
         1 == exchanges?.size() // wrong number of exchanges
        'my.topic' == exchanges[0].name // wrong exchange name
        TopicExchange == exchanges[0].type // wrong exchange type
        false == exchanges[0].durable // exchange is durable
        null == exchanges[0].autoDelete // exchange has auto-delete set
        null == exchanges[0].arguments // no arguments expected

        when:
        def queues = builder.queues

        then:
        2 == queues?.size() // wrong number of queues
        'foo' == queues[0].name // wrong queue name
        true == queues[0].durable // wrong durable value
        false == queues[0].autoDelete // queue is auto delete
        false == queues[0].exclusive // queue is exclusive
        null == queues[0].arguments // no arguments expected
        'bar' == queues[1].name // wrong queue name
        false == queues[1].durable // wrong durable value
        true == queues[1].autoDelete // queue is not auto delete
        false == queues[1].exclusive // queue is exclusive
        null == queues[1].arguments // no arguments expected

        when:
        def bindings = builder.bindings

        then:
        2 == bindings?.size() // wrong number of bindings
        'my.topic' == bindings[0].exchange // wrong exchange bound
        'foo' == bindings[0].queue // wrong queue bound
        'shares.#' == bindings[0].rule // wrong binding rule
        'my.topic' == bindings[1].exchange // wrong exchange bound
        'bar' == bindings[1].queue // wrong queue bound
        'shares.nyse.?' == bindings[1].rule // wrong binding rule
        null == bindings[0].arguments // no arguments expected
    }

    void testStandaloneTopicExchangeDefinition() {
        when:
        def closure = {
            exchange name: 'another.topic', type: topic
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() // wrong number of exchanges
        'another.topic' == exchanges[0].name // wrong exchange name
        TopicExchange == exchanges[0].type // wrong exchange type
        null == exchanges[0].durable // durable is set
        null == exchanges[0].autoDelete // auto delete is set

        0 == builder.queues?.size() // wrong number of queues
        0 == builder.bindings?.size() // wrong number of bindings
    }

    void testTopicExchangeDefinitionWithNoBinding() {
        when:
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

        then:
        msg.contains("queue 'foo'") // wrong exception message
        msg.contains("exchange 'bad.topic'") // wrong exception message
        msg.contains("must be declared") // wrong exception message
    }

    void testTopicExchangeDefinitionWithInvalidBinding() {
        when:
        def closure = {
            exchange name: 'bad.topic', type: topic, {
                foo durable: false, exclusive: true, binding: [key: 'shares.#']
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder

        def msg = shouldFail(RuntimeException) {
            closure()
        }

        then:
        msg.contains("queue 'foo'") // wrong exception message
        msg.contains("exchange 'bad.topic'") // wrong exception message
        msg.contains("must be a string") // wrong exception message
    }

    void testDirectExchangeDefinitionWithImplicitBinding() {
        when:
        def closure = {
            exchange name: 'my.direct', type: direct, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() // wrong number of exchanges
        'my.direct' == exchanges[0].name // wrong exchange name
        DirectExchange == exchanges[0].type // wrong exchange type
        true == exchanges[0].durable // exchange is not durable
        null == exchanges[0].autoDelete // exchange has auto-delete set
        null == exchanges[0].arguments // no arguments expected

        when:
        def queues = builder.queues

        then:
        1 == queues?.size() // wrong number of queues
        'foo' == queues[0].name // wrong queue name
        false == queues[0].durable // wrong durable value
        true == queues[0].autoDelete // wrong auto delete value
        true == queues[0].exclusive // wrong exclusive value
        null == queues[0].arguments // no arguments expected

        when:
        def bindings = builder.bindings

        then:
        1 == bindings?.size() // wrong number of bindings
        'my.direct' == bindings[0].exchange // wrong exchange bound
        'foo' == bindings[0].queue // wrong queue bound
        'foo' == bindings[0].rule // binding is wrong
        null == bindings[0].arguments // no arguments expected
    }

    void testDirectExchangeDefinitionWithExplicitBinding() {
        when:
        def closure = {
            exchange name: 'my.direct', type: direct, durable: true, {
                foo autoDelete: true, durable: false, exclusive: true, binding: 'shares.nyse.vmw'
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() // wrong number of exchanges
        'my.direct' == exchanges[0].name // wrong exchange name
        DirectExchange == exchanges[0].type // wrong exchange type
        true == exchanges[0].durable // exchange is not durable
        null == exchanges[0].autoDelete // exchange has auto-delete set
        null == exchanges[0].arguments // no arguments expected

        when:
        def queues = builder.queues

        then:
        1 == queues?.size() // wrong number of queues
        'foo' == queues[0].name // wrong queue name
        false == queues[0].durable // wrong durable value
        true == queues[0].autoDelete // wrong auto delete value
        true == queues[0].exclusive // wrong exclusive value
        null == queues[0].arguments // no arguments expected

        when:
        def bindings = builder.bindings

        then:
        1 == bindings?.size() // wrong number of bindings
        'my.direct' == bindings[0].exchange // wrong exchange bound
        'foo' == bindings[0].queue // wrong queue bound
        'shares.nyse.vmw' == bindings[0].rule // binding is wrong
        null == bindings[0].arguments // no arguments expected
    }

    void testStandaloneDirectExchangeDefinition() {
        when:
        def closure = {
            exchange name: 'my.direct', type: direct, durable: false, autoDelete: true
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def exchanges = builder.exchanges

        then:
        1 == exchanges?.size() // wrong number of exchanges
        'my.direct' == exchanges[0].name // wrong exchange name
        DirectExchange == exchanges[0].type // wrong exchange type
        false == exchanges[0].durable // exchange is durable
        true == exchanges[0].autoDelete // exchange does not have auto-delete set
        null == exchanges[0].arguments // no arguments expected

        0 == builder.queues?.size() // wrong number of queues
        0 == builder.bindings?.size() // wrong number of bindings
    }

    void testDirectExchangeDefinitionWithInvalidBinding() {
        when:
        def closure = {
            exchange name: 'bad.direct', type: direct, {
                foo durable: false, exclusive: true, binding: [key: 'shares.#']
            }
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder

        def msg = shouldFail(RuntimeException) {
            closure()
        }

        then:
        msg.contains("queue 'foo'") // wrong exception message
        msg.contains("exchange 'bad.direct'") // wrong exception message
        msg.contains("must be a string") // wrong exception message
    }
}
