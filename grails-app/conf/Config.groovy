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

grails.doc.authors = 'Jeff Brown'
grails.doc.license = 'Apache License 2.0'
grails.doc.title = 'RabbitMQ Plugin'
