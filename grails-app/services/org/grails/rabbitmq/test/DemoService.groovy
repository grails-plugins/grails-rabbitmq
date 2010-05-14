package org.grails.rabbitmq.test

class DemoService {
    static rabbitQueue = 'foo'

    void handleMessage(String textMessage) {
        println "Received Message: ${textMessage}"
    }
}