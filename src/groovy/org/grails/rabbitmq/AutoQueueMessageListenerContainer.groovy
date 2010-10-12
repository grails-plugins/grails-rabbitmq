package org.grails.rabbitmq

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
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
    Object exchange

    protected void doStart() {
        // First, create a broker-named, temporary queue.
        def adminBean = applicationContext.getBean(rabbitAdminBeanName)
        def queue = adminBean.declareQueue()
        
        // Get the details of the exchange to bind the temporary queue to.
        def exchangeName = exchange
        def routingKey = '#'
        if (exchange instanceof Map) {
            exchangeName = exchange.name
            routingKey = exchange.routingKey ?: routingKey
        }
        else if (!(exchange instanceof CharSequence)) {
            log.error "Property [exchange] must be a string or a map - current value: ${exchange.getClass()}"
            return
        }
        
        // Now bind this queue to the named exchanged. If the exchange is a
        // fanout, then we don't bind with a routing key. If it's a topic,
        // we use the 'match-all' wildcard. Other exchange types are not
        // supported.
        def exchange = applicationContext.getBean("grails.rabbit.exchange.${exchangeName}")
        def binding = null
        if (exchange instanceof FanoutExchange) {
            binding = new Binding(queue, exchange)
            
            if (exchange instanceof Map && exchange.routingKey) {
                log.warn "Routing key ignored for fanout exchange '${exchangeName}'"
            }
        }
        else if (exchange instanceof TopicExchange) {
            binding = new Binding(queue, exchange, routingKey)
        }
        else {
            log.error "Cannot subscribe to an exchange ('${exchange.name}') that is neither a fanout nor a topic"
            return
        }
        
        adminBean.declareBinding(binding)
        
        // Let the super class do the rest.
        super.setQueueName(queue.name)
        super.doStart()
    }
}
