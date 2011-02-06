package org.example

import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate

class MainTests extends AbstractTestCase {
    static transactional = false

    def adm
    def grailsApplication
    def rabbitTemplate
    def producerService

    /*
    void setUp() {
        def q = new Queue("fooTxn")
        adm.start()
        adm.declareQueue(q)
    }
    */

    void tearDown() {
        Status.executeUpdate("DELETE Status")
//        adm.deleteQueue("fooTxn")
//        adm.stop()
    }

    void testNonTransactionalWithNormalMessage() {
        assert Status.count() == 0

        producerService.sendNonTxnMessage("Hello world")

        assert tryUntil(500, 5000) {
            Status.count() == 1
        }, "Status message has not been created."
    }

    void testNonTransactionalWithError() {
        assert Status.count() == 0

        producerService.sendNonTxnMessage("throw exception")

        Thread.sleep(1000)

        assert Status.count() == 0, "A status message has been saved when it shouldn't have been."

        // Check that the message has *not* remained on the queue.
        def response = rabbitTemplate.getChannel(rabbitTemplate.transactionalResourceHolder).basicGet("fooNonTxn", true)
        assert response == null
    }

    void testTransactionalWithNormalMessage() {
        assert Status.count() == 0

        producerService.sendTxnMessage("Hello world")

        assert tryUntil(500, 5000) {
            Status.count() == 1
        }, "Status message has not been created."
    }

    void testTransactionalWithError() {
        assert Status.count() == 0

        producerService.sendTxnMessage("throw exception")

        Thread.sleep(1000)

        assert Status.count() == 0, "A status message has been saved when it shouldn't have been."

        // Stop the service that's consuming the messages so we can check that
        // the message is still on the queue.
        def listener = grailsApplication.mainContext.getBean("txnService_MessageListenerContainer")
        listener.stop()

        Thread.sleep(500)

        // Check that the message is back on the queue.
        def msg = rabbitTemplate.receiveAndConvert("fooTxn")
        assert msg == "throw exception"
    }
}
