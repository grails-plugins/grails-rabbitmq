package org.example

import org.example.messages.PersonMessage

class ConsumerService {
    static rabbitQueue = "personTestQueue"

    void handleMessage(PersonMessage msg) {
        new Person(name: msg.name, age: msg.age).save(failOnError: true, flush: true)
    }
}
