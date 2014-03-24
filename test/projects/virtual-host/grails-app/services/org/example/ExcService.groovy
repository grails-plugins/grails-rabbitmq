package org.example

import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties

class ExcService {
	static rabbitQueue = [queues: 'exc', messageConverterBean: '']
	
	void handleMessage(Message message) {
		String value = new String(message.body)
		
		if(message.messageProperties.contentType != MessageProperties.CONTENT_TYPE_TEXT_PLAIN ){
			log.error("Unsupported Message contentType ${message.messageProperties.contentType} for message ${value}")
			return
		}
		
		if(value.contains('Exception')){
			// should requeue here
			throw new Exception(value)
		}
		
		log.info("Received $value")
	}
	
	/*
	 * This method is fired every time there is an exception; before a message is
	 * eventually routed to a dead letter queue or dropped if no DLQ configured
	 */
	void handleError(Throwable t){
		log.info("We don't really care about dropping these, so, only info here.", t)
	}
}
