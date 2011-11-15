package org.grails.rabbitmq

import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.commons.GrailsClass

class RabbitConfigurationHolderTest extends GrailsUnitTestCase {

    def disabledConfig = toConfigHolder("""
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

    def enabledConfig = toConfigHolder("""
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

    def blueService = [getPropertyName: {'blueService'}] as GrailsClass
    def redService = [getPropertyName: {'redService'}] as GrailsClass
    def pinkService = [getPropertyName: {'pinkService'}] as GrailsClass

    def toConfigHolder(String config) {
        return new RabbitConfigurationHolder(new ConfigSlurper().parse(config).rabbitmq)
    }

    void testGetDefaultConcurrentConsumers() {
        assert disabledConfig.defaultConcurrentConsumers == 2
        assert enabledConfig.defaultConcurrentConsumers == 1
    }

    void testServiceConcurrentConsumers() {
        assert disabledConfig.getServiceConcurrentConsumers(redService) == 3
        assert disabledConfig.getServiceConcurrentConsumers(blueService) == 2
        assert disabledConfig.getServiceConcurrentConsumers(pinkService) == 2

        assert enabledConfig.getServiceConcurrentConsumers(redService) == 5
        assert enabledConfig.getServiceConcurrentConsumers(blueService) == 1
        assert enabledConfig.getServiceConcurrentConsumers(pinkService) == 1
    }

    void testListeningDisabled() {
        assert disabledConfig.listeningDisabled
        assert !enabledConfig.listeningDisabled
    }

    void testServiceEnabled() {
        assert !disabledConfig.isServiceEnabled(redService)
        assert !disabledConfig.isServiceEnabled(blueService)
        assert !disabledConfig.isServiceEnabled(pinkService)

        assert enabledConfig.isServiceEnabled(redService)
        assert !enabledConfig.isServiceEnabled(blueService)
        assert enabledConfig.isServiceEnabled(pinkService)
    }

}
