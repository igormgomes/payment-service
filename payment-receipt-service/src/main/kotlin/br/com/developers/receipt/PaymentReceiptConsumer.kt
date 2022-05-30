package br.com.developers.receipt

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.listener.Acknowledgment
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import javax.validation.Valid

@Component
@Validated
class PaymentReceiptConsumer(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SqsListener(value = ["\${payment.receipt.queue-name}"], deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    fun listen(
        @Valid request: PaymentReceiptSnsRequest,
        @Headers messageHeaders: MessageHeaders,
        acknowledgment: Acknowledgment
    ) {
        log.info("Receiving message payment receipt $request")

        val paymentReceiptRequest = this.objectMapper.readValue(request.message, PaymentReceiptRequest::class.java)
        log.info("Payment receipt $paymentReceiptRequest")

        acknowledgment.acknowledge()
    }
}