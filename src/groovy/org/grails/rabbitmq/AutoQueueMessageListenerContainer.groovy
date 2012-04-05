package org.grails.rabbitmq

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Message listener container that creates a temporary, auto-delete queue and
 * binds it to a configured exchange.
 */
class AutoQueueMessageListenerContainer extends SimpleMessageListenerContainer implements ApplicationContextAware {
    private static final log = LoggerFactory.getLogger(AutoQueueMessageListenerContainer)

    String rabbitAdminBeanName = "adm"
    ApplicationContext applicationContext

    /**
     * The exchange to bind the temporary queue to. This can be a string
     * containing the name of the exchange, or a map containing the name
     * of the exchange (key: 'name') and the routing key (key: 'routing').
     * If no routing key is specified, the match-all wildcard ('#') is used.
     */
    String exchangeBeanName

    /**
     * The routing key to bind the queue to the exchange with. This is
     * the 'match-all' wildcard by default: '#'.
     */
    String routingKey = '#'

    protected void doStart() {
        // Check the exchange name has been specified.
        if (!exchangeBeanName) {
            log.error "Property [exchangeBeanName] must have a value!"
            return
        }

        // First, create a broker-named, temporary queue.
        def adminBean = applicationContext.getBean(rabbitAdminBeanName)
        def queue = adminBean.declareQueue()

        // Now bind this queue to the named exchanged. If the exchange is a
        // fanout, then we don't bind with a routing key. If it's a topic,
        // we use the 'match-all' wildcard. Other exchange types are not
        // supported.
        def exchange = applicationContext.getBean(exchangeBeanName)

        def binding = null
        if (exchange instanceof FanoutExchange) {
            binding = BindingBuilder.bind(queue).to(exchange);
        }
        else if (exchange instanceof DirectExchange || exchange instanceof TopicExchange) {
            binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        }
        else {
            log.error "Cannot subscribe to an exchange ('${exchange.name}') that is neither a fanout nor a topic"
            return
        }

        adminBean.declareBinding(binding)

        // Let the super class do the rest.
        super.setQueueNames(queue.name)
        super.doStart()
    }
}
