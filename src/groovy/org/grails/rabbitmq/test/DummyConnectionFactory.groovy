package org.grails.rabbitmq.test

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import org.springframework.amqp.rabbit.connection.ConnectionFactory

class DummyConnectionFactory implements ConnectionFactory {
    def username
    def password
    def channelCacheSize

    DummyConnectionFactory(String hostName) {}

    Connection createConnection() {
        def mockChannel = [basicConsume: {Object[] args ->}, queueDeclarePassive: {}, queueDeclare: { Object[] args ->}, basicQos: { Object[] args ->}] as Channel
        def mockConnection = [createChannel: {-> mockChannel}] as Connection
        mockConnection
    }

    String getVirtualHost(){}
    String getHost(){}
}