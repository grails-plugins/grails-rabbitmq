package org.example

import org.example.messages.PersonMessage

class ProducerService {
    void sendPersonMessage(String name, int age) {
        rabbitSend "tpq", new PersonMessage(name, age)
    }
}
