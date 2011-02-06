package org.example

class NonTxnService {
    static transactional = false

    static rabbitQueue = "fooNonTxn"

    void handleMessage(String msg) {
        println "[NonTxnService] Message received: $msg"
        if (msg == "throw exception") {
            throw new RuntimeException()
        }
        else {
            // As the service is non-transactional, we'll have to save our status message
            // in a new Hibernate session.
            Status.withNewSession {
                try {
                    new Status(message: msg).save(failOnError: true, flush: true)
                }
                catch (Exception ex) {
                    ex.printStackTrace()
                }
            }
        }
    }
}
