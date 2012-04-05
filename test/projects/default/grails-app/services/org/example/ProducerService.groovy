package org.example

import org.example.messages.PersonMessage

class ProducerService {
    void sendPersonMessage(String name, int age) {
        rabbitSend "personTestQueue", new PersonMessage(name, age)
    }

    void sendNonTxnMessage(String msg) {
        rabbitSend "fooNonTxn", msg
    }

    void sendTxnMessage(String msg) {
        rabbitSend "fooTxn", msg
    }
}
