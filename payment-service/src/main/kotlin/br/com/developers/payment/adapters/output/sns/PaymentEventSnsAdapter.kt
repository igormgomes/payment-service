package br.com.developers.payment.adapters.output.sns

import br.com.developers.payment.application.port.output.PaymentEventPublisherPort
import br.com.developers.payment.domain.PaymentEvent
import io.awspring.cloud.sns.core.SnsTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class PaymentEventSnsAdapter(
    private val snsTemplate: SnsTemplate,
    @Value("\${payment.topic.name}")
    private val topicName: String
) : PaymentEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: PaymentEvent) {
        val paymentEventRequest = event.toRequest()

        kotlin.runCatching {
            val message: Message<PaymentEventRequest> = MessageBuilder.withPayload(paymentEventRequest)
                .setHeader("event_type", paymentEventRequest.eventType.orEmpty())
                .build()
            log.info("Converted payment ${paymentEventRequest.id}_${paymentEventRequest.eventType}")
            // The Message itself is the payload, so the topic receives {"payload": {...}, "headers": {...}}.
            // payment-receipt-service reads that shape (PaymentEventIT fixes it), so do not change it here.
            this.snsTemplate.convertAndSend(this.topicName, message)

            log.info("Payment published ${paymentEventRequest.id}_${paymentEventRequest.eventType}")
        }.onFailure {
            log.error("Error to publish event ${paymentEventRequest.id}", it)
        }
    }

    private fun PaymentEvent.toRequest() = PaymentEventRequest(
        id = this.id.toString(),
        eventType = this.eventType.name,
        date = this.date.toString(),
        pixKeyCredit = this.pixKeyCredit
    )
}
