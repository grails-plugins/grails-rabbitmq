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
        def mockChannel = [close:{->},basicConsume:{Object[] args->},basicQos:{Object[] args->},queueDeclare:{Object[] args->}] as Channel
        def mockConnection = [close:{->},createChannel:{-> mockChannel}] as Connection
        mockConnection
    }
}