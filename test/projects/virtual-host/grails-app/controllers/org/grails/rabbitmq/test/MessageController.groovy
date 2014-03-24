package org.grails.rabbitmq.test

class MessageController {
    
    def index(){ }
    
    def sendMessage(){
        def msg = params.msg
        
        rabbitSend 'foo', "Message: ${msg}"
        
        def messageMap = [msgBody: msg, msgTime: new Date()]
        
        rabbitSend 'foo', messageMap
        
        redirect action: 'index'
    }
	
	def exc(){
		rabbitSend 'exc', params.message
		render 'Put a message on a queue that should generate an exception and be handled'
	}
}