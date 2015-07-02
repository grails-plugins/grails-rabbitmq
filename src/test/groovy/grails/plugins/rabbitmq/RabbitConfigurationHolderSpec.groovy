package grails.plugins.rabbitmq

import grails.core.GrailsClass
import spock.lang.Shared
import spock.lang.Specification

class RabbitConfigurationHolderSpec extends Specification {

    @Shared def disabledConfig = toConfigHolder("""
        rabbitmq {
            disableListening = true
            concurrentConsumers = 2
            services {
                redService {
                    concurrentConsumers = 3
                    disableListening = false
                }
                blueService {
                    disableListening = true
                }
            }
        }
    """)

    @Shared def enabledConfig = toConfigHolder("""
        rabbitmq {
            services {
                redService {
                    concurrentConsumers = 5
                    disableListening = false
                }
                blueService {
                    disableListening = true
                }
            }
        }
    """)

    @Shared def blueService = [getPropertyName: {'blueService'}] as GrailsClass
    @Shared def redService = [getPropertyName: {'redService'}] as GrailsClass
    @Shared def pinkService = [getPropertyName: {'pinkService'}] as GrailsClass

    def toConfigHolder(String config) {
        return new RabbitConfigurationHolder(new ConfigSlurper().parse(config).rabbitmq)
    }

    void testGetDefaultConcurrentConsumers() {
        expect:
        disabledConfig.defaultConcurrentConsumers == 2
        enabledConfig.defaultConcurrentConsumers == 1
    }

    void testServiceConcurrentConsumers() {
        expect:
        disabledConfig.getServiceConcurrentConsumers(redService) == 3
        disabledConfig.getServiceConcurrentConsumers(blueService) == 2
        disabledConfig.getServiceConcurrentConsumers(pinkService) == 2

        enabledConfig.getServiceConcurrentConsumers(redService) == 5
        enabledConfig.getServiceConcurrentConsumers(blueService) == 1
        enabledConfig.getServiceConcurrentConsumers(pinkService) == 1
    }

    void testListeningDisabled() {
        expect:
        disabledConfig.listeningDisabled
        !enabledConfig.listeningDisabled
    }

    void testServiceEnabled() {
        expect:
        !disabledConfig.isServiceEnabled(redService)
        !disabledConfig.isServiceEnabled(blueService)
        !disabledConfig.isServiceEnabled(pinkService)

        enabledConfig.isServiceEnabled(redService)
        !enabledConfig.isServiceEnabled(blueService)
        enabledConfig.isServiceEnabled(pinkService)
    }

}
