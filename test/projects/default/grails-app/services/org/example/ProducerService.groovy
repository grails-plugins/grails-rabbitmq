package org.example

import org.example.messages.PersonMessage

class ProducerService {
    void sendPersonMessage(String name, int age) {
        rabbitSend "personTestQueue", new PersonMessage(name, age)
    }
}
