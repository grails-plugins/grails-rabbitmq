package grails.plugins.rabbitmq

import grails.core.GrailsApplication
import grails.spring.BeanBuilder
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Binding.DestinationType
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import spock.lang.Specification

/**
 * Test cases for main plugin file setup.
 */
@TestMixin(GrailsUnitTestMixin)
class RabbitGrailsPluginSpec extends Specification {

    def createPluginFileInstance(application = [:]) {
        def pluginClass = Class.forName('grails.plugins.rabbitmq.RabbitmqGrailsPlugin')
        def pluginInstance = pluginClass.newInstance()
        pluginInstance.metaClass.grailsApplication = application
        return pluginInstance
    }

    void testQueueAndExchangeSetup() {
        when:
        // mock up test configuration
        GrailsApplication application = grailsApplication

        application.metaClass.getServiceClasses = {
            return []
        }
        application.config = new ConfigSlurper().parse("""
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
        RabbitmqGrailsPlugin base = createPluginFileInstance(application)

        // run a spring builder to create context
        def bb = new BeanBuilder()
        bb.beans base.doWithSpring()
        def ctx = bb.createApplicationContext()

        // test topic
        def itTopic = ctx.getBean("grails.rabbit.exchange.it_topic")

        then:
        itTopic.getClass() == TopicExchange.class
        itTopic.durable
        !itTopic.autoDelete
        itTopic.name == 'it_topic'

        when:
        def itQ1 = ctx.getBean("grails.rabbit.queue.it_q1")

        then:
        itQ1.getClass() == Queue.class
        itQ1.name == "it_q1"
        itQ1.durable == true
        itQ1.autoDelete == false
        itQ1.arguments['x-ha-policy'] == 'all'

        // test binding
        when:
        def ibBind = ctx.getBean("grails.rabbit.binding.it_topic.it_q1")

        then:
        ibBind.getClass() == Binding.class
        ibBind.destination == 'it_q1'
        ibBind.exchange == 'it_topic'
        ibBind.routingKey == '#'
        ibBind.destinationType == DestinationType.QUEUE
    }

    void testServiceDisabling() {
        when:
        def mockBlueService = new MockQueueService(propertyName: 'blueService')
        def mockPinkService = new MockQueueService(propertyName: 'pinkService')
        def mockRedService = new MockSubscribeService(propertyName: 'redService')
        def mockTealService = new MockSubscribeService(propertyName: 'tealService')

        GrailsApplication application = grailsApplication
        application.metaClass.getServiceClasses = { -> [mockBlueService, mockRedService, mockPinkService, mockTealService] }

        application.config = new ConfigSlurper().parse("""
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

        RabbitmqGrailsPlugin base = createPluginFileInstance(application)

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
        bb.beans(base.doWithSpring())
        def ctx = bb.createApplicationContext()

        then:
        ctx.getBean('blueService_MessageListenerContainer').concurrentConsumers == 5
        ctx.getBean('pinkService_MessageListenerContainer').concurrentConsumers == 1

        when:
        shouldFail(NoSuchBeanDefinitionException) {
            ctx.getBean('redService_MessageListenerContainer')
        }

        then:
        ctx.getBean('tealService_MessageListenerContainer')
    }

    void testQueueFailureWhenMultipleListenersHaveSameName() {
        when:
        def blueService1 = new MockQueueService(propertyName: 'blueService')
        def blueService2 = new MockQueueService(propertyName: 'blueService')
        def redService1 = new MockSubscribeService(propertyName: 'redService')
        def redService2 = new MockSubscribeService(propertyName: 'redService')

        GrailsApplication application = grailsApplication
        application.metaClass.getServiceClasses =  {
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

        RabbitmqGrailsPlugin queueServices = createPluginFileInstance(application)
        def msg = shouldFail(IllegalArgumentException) {
            new BeanBuilder().beans queueServices.doWithSpring()
        }

        then:
        msg.contains('blueService')

        when:
        application.metaClass.getServiceClasses = {
            return [redService1, redService2]
        }

        RabbitmqGrailsPlugin subscribeServices = createPluginFileInstance(application)
        msg = shouldFail(IllegalArgumentException) {
            new BeanBuilder().beans subscribeServices.doWithSpring()
        }

        then:
        msg.contains('redService')
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
