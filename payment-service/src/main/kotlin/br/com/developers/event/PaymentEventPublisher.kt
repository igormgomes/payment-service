package br.com.developers.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sns.core.SnsTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service

@Service
class PaymentEventPublisher(
    private val snsTemplate: SnsTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${payment.topic.name}")
    private val topicName: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(paymentEventRequest: PaymentEventRequest?) {
        checkNotNull(paymentEventRequest)

        kotlin.runCatching {
            val json = this.objectMapper.writeValueAsString(paymentEventRequest)
            val message: Message<String> = MessageBuilder.withPayload(json)
                .setHeader("event_type", paymentEventRequest.eventType.orEmpty())
                .build()
            log.info("Converted payment $message")
            this.snsTemplate.convertAndSend(this.topicName, message)

            log.info("Payment published ${paymentEventRequest.id}_${paymentEventRequest.eventType}")
        }.onFailure {
            log.error("Error to publish event ${paymentEventRequest.id}", it)
        }
    }
}