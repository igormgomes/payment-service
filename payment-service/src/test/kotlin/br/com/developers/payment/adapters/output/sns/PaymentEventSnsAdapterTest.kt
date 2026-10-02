package br.com.developers.payment.adapters.output.sns

import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.PaymentEvent
import io.awspring.cloud.sns.core.SnsTemplate
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.messaging.Message
import java.time.LocalDate
import java.util.UUID

@DisplayName("Payment event SNS adapter test")
@ExtendWith(MockitoExtension::class)
class PaymentEventSnsAdapterTest {

    private val snsTemplate: SnsTemplate = mock()

    private lateinit var paymentEventSnsAdapter: PaymentEventSnsAdapter

    @BeforeEach
    fun before() {
        this.paymentEventSnsAdapter = PaymentEventSnsAdapter(this.snsTemplate, "topic-test")
    }

    @Test
    fun `Should test the published payment`() {
        val id = UUID.randomUUID()
        val date = LocalDate.now()
        val paymentEvent = PaymentEvent(
            id = id,
            eventType = EventType.SCHEDULED_PAYMENT.name,
            date = date,
            pixKeyCredit = "123"
        )

        this.paymentEventSnsAdapter.publish(paymentEvent)

        val argumentCaptor = argumentCaptor<Message<*>>()
        verify(this.snsTemplate, times(1)).convertAndSend(eq("topic-test"), argumentCaptor.capture())
        val message = argumentCaptor.firstValue
        assertAll("Assert published message", {
            assertThat(
                message.payload,
                `is`(equalTo<Any>(PaymentEventRequest(id.toString(), EventType.SCHEDULED_PAYMENT.name, date.toString(), "123")))
            )
            assertThat(message.headers["event_type"], `is`(equalTo<Any>(EventType.SCHEDULED_PAYMENT.name)))
        })
    }

    @Test
    fun `Should test the published payment with error`() {
        val paymentEvent = PaymentEvent(
            id = UUID.randomUUID(),
            eventType = EventType.SCHEDULED_PAYMENT.name,
            date = LocalDate.now(),
            pixKeyCredit = "123"
        )
        doThrow(RuntimeException("sns is down"))
            .whenever(this.snsTemplate).convertAndSend(eq("topic-test"), any<Any>())

        assertDoesNotThrow {
            this.paymentEventSnsAdapter.publish(paymentEvent)
        }

        verify(this.snsTemplate, times(1)).convertAndSend(eq("topic-test"), any<Any>())
    }

    @Test
    fun `Should publish an empty event type header when the event has no type`() {
        val paymentEvent = PaymentEvent(id = UUID.randomUUID(), eventType = null, date = null, pixKeyCredit = null)

        this.paymentEventSnsAdapter.publish(paymentEvent)

        val argumentCaptor = argumentCaptor<Message<*>>()
        verify(this.snsTemplate, times(1)).convertAndSend(eq("topic-test"), argumentCaptor.capture())
        assertThat(argumentCaptor.firstValue.headers["event_type"], `is`(equalTo<Any>("")))
    }
}
