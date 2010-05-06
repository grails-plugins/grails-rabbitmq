class DemoController {
    
    def index = {
        rabbitSend 'foo', "message: ${params.msg}"
        
        render 'it worked'
    }
}