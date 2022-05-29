package br.com.developers.payment.event

import br.com.developers.event.PaymentEventPublisher
import br.com.developers.event.PaymentEventRequest
import br.com.developers.payment.EventType
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@DisplayName("Payment publisher test")
@ExtendWith(MockitoExtension::class)
class PaymentEventPublisherTest {

    private val notificationMessagingTemplate: NotificationMessagingTemplate = mock()

    private lateinit var paymentEventPublisher: PaymentEventPublisher

    @BeforeEach
    fun before() {
        this.paymentEventPublisher = PaymentEventPublisher(this.notificationMessagingTemplate, "topic-test")
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
        val paymentEventRequest =
            PaymentEventRequest(id = "id", eventType = EventType.SCHEDULED_PAYMENT.name, pixKey = "123")

        this.paymentEventPublisher.publish(paymentEventRequest)

        verify(this.notificationMessagingTemplate, atLeastOnce()).convertAndSend(
            eq("topic-test"),
            eq(paymentEventRequest)
        )
    }

    @Test
    fun `Should test the published payment with error`() {
        val paymentEventRequest =
            PaymentEventRequest(id = "id", eventType = EventType.SCHEDULED_PAYMENT.name, pixKey = "123")
        whenever(this.notificationMessagingTemplate.convertAndSend(eq("topic-test"), eq(paymentEventRequest)))
            .thenThrow(RuntimeException("Internal error"))

        this.paymentEventPublisher.publish(paymentEventRequest)

        verify(this.notificationMessagingTemplate, atLeastOnce()).convertAndSend(
            eq("topic-test"),
            eq(paymentEventRequest)
        )
    }
}