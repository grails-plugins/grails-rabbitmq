package org.grails.rabbitmq.test

class MessageController {
    
    def index = {}
    
    def sendMessage = {
        def msg = params.msg
        
        rabbitSend 'foo', "Message: ${msg}"
        
        redirect action: index
    }
}