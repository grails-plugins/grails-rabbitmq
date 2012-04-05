package org.grails.rabbitmq

class RabbitConfigurationHolder {

    def rabbitmqConfig

    public RabbitConfigurationHolder(rabbitmqConfig) {
        this.rabbitmqConfig = rabbitmqConfig
    }

    int getDefaultConcurrentConsumers() {
        return rabbitmqConfig.concurrentConsumers ?: 1
    }

    int getServiceConcurrentConsumers(def service) {
        def propertyName = service.propertyName
        return rabbitmqConfig.services?."${propertyName}"?.concurrentConsumers ?: defaultConcurrentConsumers
    }

    boolean isListeningDisabled() {
        return rabbitmqConfig.disableListening
    }

    boolean isServiceEnabled(def service) {
        if (listeningDisabled) return false
        def propertyName = service.propertyName
        return !rabbitmqConfig.services?."${propertyName}"?.disableListening
    }

}
