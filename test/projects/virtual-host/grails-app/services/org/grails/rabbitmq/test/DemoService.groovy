package org.grails.rabbitmq.test

class DemoService {
    static rabbitQueue = 'foo'

    void handleMessage(String textMessage) {
        println "Received Message: ${textMessage}"
    }

    void handleMessage(Map mapMessage) {
        println "Received Map Message..."
        mapMessage?.each { key, val ->
            println "   ${key}: ${val}"
        }
    }
}