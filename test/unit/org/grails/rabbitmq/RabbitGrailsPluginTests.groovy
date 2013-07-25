package org.grails.rabbitmq

import grails.spring.BeanBuilder
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Binding.DestinationType
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.codehaus.groovy.grails.commons.GrailsClass

/**
 * Test cases for main plugin file setup.
 */
class RabbitGrailsPluginTests extends GroovyTestCase {

    def createPluginFileInstance(application=[:]) {
        String[] roots = ['./']
        ClassLoader loader = this.getClass().getClassLoader()
        def engine = new GroovyScriptEngine(roots, loader)
        def pluginClass = engine.loadScriptByName('RabbitmqGrailsPlugin.groovy')
        def pluginInstance = pluginClass.newInstance()
        pluginInstance.metaClass.application = application
        return pluginInstance
    }

    void testQueueAndExchangeSetup() {
        // mock up test configuration
        def application = new Object()

        application.metaClass.getServiceClasses = {
            return []
        }
        application.metaClass.config =  new ConfigSlurper().parse("""
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
        def base = createPluginFileInstance(application)

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

    void testServiceDisabling() {
        def mockBlueService = new MockQueueService(propertyName: 'blueService')
        def mockPinkService = new MockQueueService(propertyName: 'pinkService')
        def mockRedService = new MockSubscribeService(propertyName: 'redService')
        def mockTealService = new MockSubscribeService(propertyName: 'tealService')

        def application = new Object()

        application.metaClass.getServiceClasses = {
            return [mockBlueService, mockRedService, mockPinkService, mockTealService]
        }

        application.metaClass.config = new ConfigSlurper().parse("""
            rabbitmq {
                connectionfactory {
                    username = 'guest'
                    password = 'guest'
                    hostname = 'localhost'
                }
                services {
                    blueService {
                        concurrentConsumers = 5
                        disableListening = false
                    }
                    redService {
                        concurrentConsumers = 4
                        disableListening = true
                    }
                }
            }
        """)

        def base = createPluginFileInstance(application)

        def bb = new BeanBuilder()
        bb.beans {
            pinkService(MockQueueService) {
                propertyName = 'pinkService'
            }
            blueService(MockQueueService) {
                propertyName = 'blueService'
            }
            tealService(MockQueueService) {
                propertyName = 'tealService'
            }
        }
        bb.beans base.doWithSpring
        def ctx = bb.createApplicationContext()

        assert ctx.getBean('blueService_MessageListenerContainer').concurrentConsumers == 5
        assert ctx.getBean('pinkService_MessageListenerContainer').concurrentConsumers == 1

        shouldFail(NoSuchBeanDefinitionException) {
            ctx.getBean('redService_MessageListenerContainer')
        }
        assert ctx.getBean('tealService_MessageListenerContainer')
    }

    void testQueueFailureWhenMultipleListenersHaveSameName() {
        def blueService1 = new MockQueueService(propertyName: 'blueService')
        def blueService2 = new MockQueueService(propertyName: 'blueService')
        def redService1 = new MockSubscribeService(propertyName: 'redService')
        def redService2 = new MockSubscribeService(propertyName: 'redService')

        def application = new Object()
        application.metaClass.getServiceClasses = {
            return [blueService1, blueService2]
        }

        application.metaClass.config = new ConfigSlurper().parse("""
            rabbitmq {
                connectionfactory {
                    username = 'guest'
                    password = 'guest'
                    hostname = 'localhost'
                }
            }
        """)
        def queueServices = createPluginFileInstance(application)

        def msg = shouldFail(IllegalArgumentException){
            new BeanBuilder().beans queueServices.doWithSpring
        }
        assert msg.contains('blueService')

        application.metaClass.getServiceClasses = {
            return [redService1, redService2]
        }

        def subscribeServices = createPluginFileInstance(application)
        msg = shouldFail(IllegalArgumentException){
            new BeanBuilder().beans subscribeServices.doWithSpring
        }
        assert msg.contains('redService')
    }

}

class MockSubscribeService {
    static rabbitSubscribe = 'blueExchange'
    static transactional = false
    def propertyName
    def clazz = MockSubscribeService.class
}

class MockQueueService {
    static rabbitQueue = 'blueQueue'
    static transactional = false
    def propertyName
    def clazz = MockQueueService.class
}
