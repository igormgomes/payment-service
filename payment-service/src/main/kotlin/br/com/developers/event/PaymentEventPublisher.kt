package br.com.developers.event

import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PaymentEventPublisher(
    private val notificationMessagingTemplate: NotificationMessagingTemplate,
    @Value("\${payment.topic.name}")
    private val topicName: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(paymentEventRequest: PaymentEventRequest?) {
        checkNotNull(paymentEventRequest)

        kotlin.runCatching {
            this.notificationMessagingTemplate.convertAndSend(this.topicName, paymentEventRequest)

            log.info("Payment published ${paymentEventRequest.id}_${paymentEventRequest.eventType}")
        }.onFailure {
            log.error("Error to publish event ${paymentEventRequest.id}", it)
        }
    }
}