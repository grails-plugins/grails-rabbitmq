package org.example

class TxnService {
    static transactional = true

    static rabbitQueue = "fooTxn"

    void handleMessage(String msg) {
        if (msg == "throw exception") {
            throw new RuntimeException()
        }
        else {
            try {
                new Status(message: msg).save(failOnError: true, flush: true)
            }
            catch (Exception ex) {
                ex.printStackTrace()
                throw ex
            }
        }
    }
}
