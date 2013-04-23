package org.example

import grails.test.*

class MessageSerializationTests extends AbstractTestCase {
    def producerService

    protected void tearDown() {
        super.tearDown()
        Person.executeUpdate("DELETE Person")
    }

    void testSendingSerializableMessage() {
        assert Person.count() == 0

        producerService.sendPersonMessage("Peter", 34)
        producerService.sendPersonMessage("Bob", 46)
        producerService.sendPersonMessage("Jill", 26)
        producerService.sendPersonMessage("Amy", 43)
        producerService.sendPersonMessage("Kate", 12)

        // Wait for the messages to be consumed.
        assert tryUntil(500, 10000) {
            Person.count() == 5
        }, "Person messages have not been consumed"
    }
}
