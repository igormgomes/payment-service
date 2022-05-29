package br.com.developers.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PaymentEventPublisher(
    private val notificationMessagingTemplate: NotificationMessagingTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${payment.topic.name}")
    private val topicName: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(paymentEventRequest: PaymentEventRequest?) {
        checkNotNull(paymentEventRequest)

        kotlin.runCatching {
            val json = this.objectMapper.writeValueAsString(paymentEventRequest)
            val headers = mapOf<String, Any>("event_type" to paymentEventRequest.eventType.orEmpty())
            log.info("Converted payment $json and headers $headers")

            this.notificationMessagingTemplate.convertAndSend(this.topicName, json, headers)

            log.info("Payment published ${paymentEventRequest.id}_${paymentEventRequest.eventType}")
        }.onFailure {
            log.error("Error to publish event ${paymentEventRequest.id}", it)
        }
    }
}