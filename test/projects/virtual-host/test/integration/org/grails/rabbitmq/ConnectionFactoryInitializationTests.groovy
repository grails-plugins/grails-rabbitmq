package org.grails.rabbitmq


class ConnectionFactoryInitializationTests extends GroovyTestCase {

    def rabbitMQConnectionFactory

    void testConnectionFactoryInitialization() {
        assertEquals 10, rabbitMQConnectionFactory.channelCacheSize
    }

}
