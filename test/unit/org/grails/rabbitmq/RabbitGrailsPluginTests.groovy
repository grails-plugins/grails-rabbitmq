package org.grails.rabbitmq

import grails.spring.BeanBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Binding.DestinationType

/**
 * Test cases for main plugin file setup.
 */
class RabbitGrailsPluginTests extends GroovyTestCase {

    void testQueueAndExchangeSetup(){
        // load the base plugin file
        String[] roots = ['./']
        ClassLoader loader = this.getClass().getClassLoader()
        def engine = new GroovyScriptEngine(roots, loader)
        def pluginClass = engine.loadScriptByName('RabbitmqGrailsPlugin.groovy')
        def base = pluginClass.newInstance()

        // mock up test configuration
        base.metaClass.application = [:]
        base.application.config = new ConfigSlurper().parse("""
            rabbitmq {
                connectionfactory {
                    username = 'guest'
                    password = 'guest'
                    hostname = 'localhost'
                }

                queues = {
                   exchange name: 'it_topic', durable: true, type: topic, autoDelete: false, {
                         it_q1 autoDelete: false, durable: true, binding: '#', arguments: ['x-ha-policy' : 'all']
                   }
                }
            }
        """)

        // run a spring builder to create context
        def bb = new BeanBuilder()
        bb.beans base.doWithSpring
        def ctx = bb.createApplicationContext()

        // test topic
        def itTopic = ctx.getBean("grails.rabbit.exchange.it_topic")
        assertEquals(itTopic.getClass(), TopicExchange.class)
        assertTrue(itTopic.durable)
        assertFalse(itTopic.autoDelete)
        assertEquals(itTopic.name, 'it_topic')

        // test queue
        def itQ1 = ctx.getBean("grails.rabbit.queue.it_q1")
        assertEquals(itQ1.getClass(), Queue.class)
        assertEquals(itQ1.name, "it_q1")
        assertEquals(itQ1.durable, true)
        assertEquals(itQ1.autoDelete, false)
        assertEquals(itQ1.arguments['x-ha-policy'], 'all')

        // test binding
        def ibBind = ctx.getBean("grails.rabbit.binding.it_topic.it_q1")
        assertEquals(ibBind.getClass(), Binding.class)
        assertEquals(ibBind.destination, 'it_q1')
        assertEquals(ibBind.exchange, 'it_topic')
        assertEquals(ibBind.routingKey, '#')
        assertEquals(ibBind.destinationType, DestinationType.QUEUE)
    }

}
