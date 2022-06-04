package br.com.developers.payment

import br.com.developers.receipt.PaymentReceipt
import br.com.developers.receipt.PaymentReceiptConsumer
import br.com.developers.receipt.PaymentReceiptService
import br.com.developers.receipt.PaymentReceiptSnsRequest
import br.com.developers.receipt.PaymentReceiptRequest
import br.com.developers.receipt.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.listener.Acknowledgment
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

@DisplayName("Payment receipt consumer test")
class PaymentReceiptConsumerTest {

    private val objectMapper: ObjectMapper = mock()
    private val paymentReceiptService: PaymentReceiptService = mock()
    private val messageHeaders: MessageHeaders = mock()
    private val acknowledgment: Acknowledgment = mock()
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
        val paymentReceiptSnsRequest = PaymentReceiptSnsRequest(message = "{}", messageId = "1")
        val paymentReceiptRequest = PaymentReceiptRequest(
            id = "1",
            eventType = "PROCESSED_PAYMENT",
            date = LocalDate.now(),
            pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32"
        )
        whenever(this.objectMapper.readValue(paymentReceiptSnsRequest.message, PaymentReceiptRequest::class.java))
            .thenReturn(paymentReceiptRequest)

        this.paymentReceiptConsumer.listen(paymentReceiptSnsRequest, this.messageHeaders, this.acknowledgment)

        verify(this.paymentReceiptService, atLeastOnce()).save(this.argumentCaptor.capture())
        assertAll("Assert payment request event", {
            assertThat(this.argumentCaptor.value.pk, `is`(notNullValue()))
            assertThat(this.argumentCaptor.value.eventType, `is`(equalTo(EventType.PROCESSED_PAYMENT)))
            assertThat(this.argumentCaptor.value.inclusionDate, `is`(equalTo(LocalDate.now())))
            assertThat(this.argumentCaptor.value.paymentDate, `is`(equalTo(paymentReceiptRequest.date)))
            assertThat(this.argumentCaptor.value.pixKeyCredit, `is`(equalTo(paymentReceiptRequest.pixKeyCredit)))
            assertThat(this.argumentCaptor.value.ttl, `is`(equalTo(notNullValue())))
        })
    }
}