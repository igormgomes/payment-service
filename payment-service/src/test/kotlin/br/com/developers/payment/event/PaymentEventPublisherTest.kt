package br.com.developers.payment.event

import br.com.developers.event.PaymentEventPublisher
import br.com.developers.event.PaymentEventRequest
import br.com.developers.payment.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sns.core.SnsTemplate
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.messaging.Message
import java.time.LocalDate

@DisplayName("Payment publisher test")
@ExtendWith(MockitoExtension::class)
class PaymentEventPublisherTest {

    private val snsTemplate: SnsTemplate = mock()
    private val objectMapper: ObjectMapper = mock()

    private lateinit var paymentEventPublisher: PaymentEventPublisher

    @BeforeEach
    fun before() {
        this.paymentEventPublisher = PaymentEventPublisher(this.snsTemplate, this.objectMapper,"topic-test")
    }

    @Test
    fun `Should test the invalid payment request`() {
        val exception = assertThrows<IllegalStateException> {
            this.paymentEventPublisher.publish(null)
        }

        assertThat(exception.message, `is`(equalTo("Required value was null.")))
    }

    @Test
    fun `Should test the published payment`() {
        val paymentEventRequest = PaymentEventRequest(
            id = "id",
            eventType = EventType.SCHEDULED_PAYMENT.name,
            date = LocalDate.now().toString(),
            pixKeyCredit = "123"
        )
        whenever(this.objectMapper.writeValueAsString(eq(paymentEventRequest)))
            .thenReturn("{}")

        this.paymentEventPublisher.publish(paymentEventRequest)

        val argumentCaptor = argumentCaptor<Message<String>>()
        verify(this.snsTemplate, atLeastOnce()).convertAndSend(
            eq("topic-test"),
            argumentCaptor.capture()
        )
    }

    @Test
    fun `Should test the published payment with error`() {
        val paymentEventRequest = PaymentEventRequest(
            id = "id",
            eventType = EventType.SCHEDULED_PAYMENT.name,
            date = LocalDate.now().toString(),
            pixKeyCredit = "123"
        )
        whenever(this.objectMapper.writeValueAsString(eq(paymentEventRequest)))
            .thenThrow(RuntimeException("Error"))

        this.paymentEventPublisher.publish(paymentEventRequest)

        verify(this.snsTemplate, never()).convertAndSend(eq("topic-test"), any())
    }
}