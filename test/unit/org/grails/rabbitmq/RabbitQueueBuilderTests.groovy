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

    void testQueueConfigurationDetails() {
        def closure = {
            foo autoDelete: true, durable: false, exclusive: true
            bar autoDelete: false, durable: true, exclusive: false
            baz autoDelete: true, arguments: [lang: 'Groovy', framework: 'Grails']
        }

        def builder = new RabbitQueueBuilder()

        closure.delegate = builder
        closure()

        def queues = builder.queues
        assertEquals 'wrong number of queues', 3, queues?.size()
        def queue = queues[0]

        assertEquals 'wrong queue name', 'foo', queue.name
        assertEquals 'wrong durable value', false, queue.durable
        assertEquals 'wrong auto delete value', true, queue.autoDelete
        assertEquals 'wrong exclusive value', true, queue.exclusive
        assertNull 'no arguments expected', queue.arguments

        queue = queues[1]

        assertEquals 'wrong queue name', 'bar', queue.name
        assertEquals 'wrong durable value', true, queue.durable
        assertEquals 'wrong auto delete value', false, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertNull 'no arguments expected', queue.arguments

        queue = queues[2]

        assertEquals 'wrong queue name', 'baz', queue.name
        assertEquals 'wrong durable value', false, queue.durable
        assertEquals 'wrong auto delete value', true, queue.autoDelete
        assertEquals 'wrong exclusive value', false, queue.exclusive
        assertEquals 'wrong number of arguments', 2, queue.arguments?.size()
        assertEquals 'lang argument had wrong value', 'Groovy', queue.arguments.lang
        assertEquals 'framework argument had wrong value', 'Grails', queue.arguments.framework
    }
}