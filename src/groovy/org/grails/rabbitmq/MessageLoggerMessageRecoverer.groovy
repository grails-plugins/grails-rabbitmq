package org.grails.rabbitmq

import groovy.util.logging.Log4j

import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.retry.MessageRecoverer

@Log4j
class MessageLoggerMessageRecoverer implements MessageRecoverer{

	@Override
	void recover(Message mesg, Throwable error) {
		log.error("Message failed all attempts at reprocessing: $mesg", error)
	}

}
