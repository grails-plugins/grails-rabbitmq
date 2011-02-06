package org.example

abstract class AbstractTestCase extends GroovyTestCase {
    boolean tryUntil(long frequency, long timeout, c) {
        long start = System.currentTimeMillis()
        while ((System.currentTimeMillis() - start) < timeout) {
            def result = c.call()
            if (result) return true
            else Thread.sleep(frequency)
        }
    }
}
