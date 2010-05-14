rabbitmq {
    connectionfactory {
        username = 'guest'
        password = 'guest'
        hostname = 'localhost'
    }
}

environments {
    test {
        rabbitmq.connectionfactory.className = 'org.grails.rabbitmq.test.DummyConnectionFactory'
    }
}