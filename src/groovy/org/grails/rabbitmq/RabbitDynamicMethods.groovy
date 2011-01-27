package org.grails.rabbitmq

import org.springframework.amqp.core.Address
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor

/**
 * Class for applying the dynamic rabbitSend() and rabbitRpcSend() methods to
 * specified classes. Makes unit testing and reuse easier.
 */
class RabbitDynamicMethods {
    
    static void applyAllMethods(target, ctx) {
        applyRabbitSend(target, ctx)
        applyRabbitRpcSend(target, ctx)
    }
    
    static void applyRabbitSend(target, ctx) {
        target.metaClass.rabbitSend = { Object[] args ->
            // The last argument of convertAndSend is of type Object so the
            // automatic conversion of GString to String doesn't happen as
            // it does for the other arguments. Since the code in that method
            // checks for String, we do the conversion manually.
            args = processArgs(args)
            ctx.rabbitTemplate.convertAndSend(*args)
        }
    }

    static void applyRabbitRpcSend(target, ctx) {
        target.metaClass.rabbitRpcSend = { Object[] args ->
            // Last argument is either a reply-queue name, a closure, or a
            // service listener.
            def newArgs = args.toList()
            def reply = newArgs.pop()
            newArgs = processArgs(newArgs)
            
            newArgs << ({ Message msg ->
                msg.messageProperties.replyTo = new Address(reply)
                return msg
            } as MessagePostProcessor)
            
            ctx.rabbitTemplate.convertAndSend(*newArgs) 
        }
    }
    
    private static processArgs(args) {
        int i = args[-1] instanceof MessagePostProcessor ? -2 : -1
        if (args[i] instanceof GString) {
            args[i] = args[i].toString()
        }
        return args
    }
}
