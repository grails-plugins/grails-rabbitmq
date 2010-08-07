package org.grails.rabbitmq

class RabbitQueueBuilderTests extends GroovyTestCase {
    
    void testSimpleQueue() {
        def closure = {
            foo()
        }
        
        def builder = new RabbitQueueBuilder()
        
        closure.delegate = builder
        closure()
        
        def queues = builder.queues
        assertEquals 'wrong number of queues', 1, queues?.size()
        def queue = queues[0]
        
        assertEquals 'wrong queue name', 'foo', queue.name
        assertEquals 'wrong durable value', false, queue.durable
        assertEquals 'wrong auto delete value', false, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertNull 'no arguments expected', queue.arguments
    }
}