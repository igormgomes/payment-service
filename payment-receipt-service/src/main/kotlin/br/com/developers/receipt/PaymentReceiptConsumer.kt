package br.com.developers.receipt

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class PaymentReceiptConsumer(
    private val objectMapper: ObjectMapper,
    private val paymentReceiptService: PaymentReceiptService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SqsListener(value = ["\${payment.receipt.queue-name}"], factory = "defaultSqsListenerContainerFactory")
    fun listen(
        @Valid request: PaymentReceiptSnsRequest?,
        @Headers messageHeaders: MessageHeaders,
        acknowledgement: Acknowledgement
    ) {
        log.info("Receiving message payment receipt $request")

        kotlin.runCatching {
            request?.message?.let {
                val paymentReceiptSnsPayloadRequest = this.objectMapper.readValue(it, PaymentReceiptSnsPayloadRequest::class.java)
                val paymentReceipt = paymentReceiptSnsPayloadRequest.payload.toPaymentReceipt()
                this.paymentReceiptService.save(paymentReceipt)
                log.info("Message processed ${paymentReceipt.pk}")
            }
        }.onSuccess { _ ->
            acknowledgement.acknowledge()
        }.onFailure { e ->
            log.error("Error processing message", e)
        }
    }
}