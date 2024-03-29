package br.com.developers.payment

import br.com.developers.receipt.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.*
import org.springframework.messaging.MessageHeaders
import java.time.LocalDate
import java.util.*

@DisplayName("Payment receipt consumer test")
class PaymentReceiptConsumerTest {

    private val objectMapper: ObjectMapper = mock()
    private val paymentReceiptService: PaymentReceiptService = mock()
    private val messageHeaders: MessageHeaders = mock()
    private val acknowledgment: Acknowledgement = mock()
    private val argumentCaptor = ArgumentCaptor.forClass(PaymentReceipt::class.java)

    private lateinit var paymentReceiptConsumer: PaymentReceiptConsumer

    @BeforeEach
    fun before() {
        this.paymentReceiptConsumer = PaymentReceiptConsumer(this.objectMapper, this.paymentReceiptService)
    }

    @Test
    fun `Should test the invalid payment receipt request`() {
        this.paymentReceiptConsumer.listen(null, this.messageHeaders, this.acknowledgment)

        verify(this.paymentReceiptService, never()).save(any())
    }

    @Test
    fun `Should test the valid payment receipt request`() {
        val paymentReceiptSnsRequest = PaymentReceiptSnsRequest(message = "{}")
        val paymentReceiptRequest = PaymentReceiptRequest(
            id = "44c7516-075b-4b52-8e90-9bb2207ce41c",
            eventType = EventType.PROCESSED_PAYMENT.name,
            date = LocalDate.now(),
            pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32"
        )
        val paymentReceiptSnsPayloadRequest = PaymentReceiptSnsPayloadRequest(payload = PaymentReceiptRequest(
            id = paymentReceiptRequest.id,
            eventType = paymentReceiptRequest.eventType,
            date = paymentReceiptRequest.date,
            pixKeyCredit = paymentReceiptRequest.pixKeyCredit))
        whenever(this.objectMapper.readValue(paymentReceiptSnsRequest.message, PaymentReceiptSnsPayloadRequest::class.java))
            .thenReturn(paymentReceiptSnsPayloadRequest)

        this.paymentReceiptConsumer.listen(paymentReceiptSnsRequest, this.messageHeaders, this.acknowledgment)

        verify(this.paymentReceiptService, atLeastOnce()).save(this.argumentCaptor.capture())
        assertAll("Assert payment request event", {
            assertThat(this.argumentCaptor.value.pk, `is`(equalTo(UUID.fromString(paymentReceiptRequest.id))))
            assertThat(this.argumentCaptor.value.status, `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
            assertThat(this.argumentCaptor.value.inclusionDate, `is`(equalTo(LocalDate.now())))
            assertThat(this.argumentCaptor.value.paymentDate, `is`(equalTo(paymentReceiptRequest.date)))
            assertThat(this.argumentCaptor.value.pixKeyCredit, `is`(equalTo(paymentReceiptRequest.pixKeyCredit)))
            assertThat(this.argumentCaptor.value.ttl, `is`(notNullValue()))
        })
    }
}