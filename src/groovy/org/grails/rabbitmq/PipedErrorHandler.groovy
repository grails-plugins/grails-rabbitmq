package org.grails.rabbitmq

import org.springframework.util.ErrorHandler


/**
 * Simple pipe to handle requirement that ErrorHandlers implement an interface
 */
class PipedErrorHandler implements ErrorHandler {

	def child
	
	@Override
	void handleError(Throwable t) {
		child.handleError(t)
	}

}
