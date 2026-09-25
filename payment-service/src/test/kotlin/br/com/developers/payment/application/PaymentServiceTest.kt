package br.com.developers.payment.application

import br.com.developers.payment.application.port.output.PaymentEventPublisherPort
import br.com.developers.payment.application.port.output.PaymentRepositoryPort
import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.Payment
import br.com.developers.payment.domain.PaymentDeletionNotAllowedException
import br.com.developers.payment.domain.PaymentEvent
import br.com.developers.payment.domain.PaymentNotFoundException
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@DisplayName("Payment service test")
@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    private val paymentRepositoryPort: PaymentRepositoryPort = mock()
    private val paymentEventPublisherPort: PaymentEventPublisherPort = mock()
    private val argumentCaptor = argumentCaptor<PaymentEvent>()

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun before() {
        this.paymentService = PaymentService(this.paymentRepositoryPort, this.paymentEventPublisherPort)
    }

    @Test
    fun `Should test the saved payment`() {
        val payment = Payment().apply {
            this.pk = UUID.fromString("44c7516-075b-4b52-8e90-9bb2207ce41d")
            this.sk = EventType.PROCESSED_PAYMENT.name
            this.date = LocalDate.now()
            this.value = BigDecimal.ONE
            this.description = "test"
            this.pixKeyCredit = "393"
        }
        whenever(this.paymentRepositoryPort.save(eq(payment)))
            .thenReturn(payment)

        this.paymentService.save(payment)

        verify(this.paymentRepositoryPort, times(1)).save(eq(payment))
        verify(this.paymentEventPublisherPort, times(1)).publish(this.argumentCaptor.capture())
        assertAll("Assert payment event", {
            assertThat(this.argumentCaptor.firstValue.id, `is`(equalTo(payment.pk)))
            assertThat(this.argumentCaptor.firstValue.eventType, `is`(equalTo(payment.sk)))
            assertThat(this.argumentCaptor.firstValue.date, `is`(equalTo(payment.date)))
            assertThat(this.argumentCaptor.firstValue.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        })
    }

    @Test
    fun `Should publish a scheduled payment event when a payment with a future date is saved`() {
        val date = LocalDate.now().plusDays(3)
        val payment = Payment.create(date, BigDecimal.TEN, "rent", "a@b.com")
        whenever(this.paymentRepositoryPort.save(eq(payment)))
            .thenReturn(payment)

        this.paymentService.save(payment)

        verify(this.paymentRepositoryPort, times(1)).save(eq(payment))
        verify(this.paymentEventPublisherPort, times(1)).publish(this.argumentCaptor.capture())
        assertAll("Assert scheduled payment event", {
            assertThat(payment.sk, `is`(equalTo(EventType.SCHEDULED_PAYMENT.name)))
            assertThat(this.argumentCaptor.firstValue.eventType, `is`(equalTo(EventType.SCHEDULED_PAYMENT.name)))
            assertThat(this.argumentCaptor.firstValue.id, `is`(equalTo(payment.pk)))
            assertThat(this.argumentCaptor.firstValue.date, `is`(equalTo(date)))
        })
    }

    @Test
    fun `Should test the not found payment in find by id`() {
        val id = "8d369a41-a278-4390-9fc8-9cd32425bf4c"
        whenever(this.paymentRepositoryPort.findByPk(id))
            .thenReturn(null)

        val exception = assertThrows<PaymentNotFoundException> {
            this.paymentService.findById(id)
        }

        assertThat(exception.message, `is`(equalTo("Payment $id not found")))
    }

    @Test
    fun `Should test the find by id`() {
        val id = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c")
        val paymentMock = Payment().apply {
            this.pk = id
            this.sk = EventType.PROCESSED_PAYMENT.name
            this.date = LocalDate.now()
            this.value = BigDecimal.ONE
            this.description = "test"
            this.pixKeyCredit = "393"
        }
        whenever(this.paymentRepositoryPort.findByPk(id.toString()))
            .thenReturn(paymentMock)

        val payment = this.paymentService.findById(id.toString())

        assertThat(payment.pk, `is`(equalTo(paymentMock.pk)))
    }

    @Test
    fun `Should test the not found payment in delete`() {
        val id = "8d369a41-a278-4390-9fc8-9cd32425bf4c"
        whenever(this.paymentRepositoryPort.findByPk(id))
            .thenReturn(null)

        val exception = assertThrows<PaymentDeletionNotAllowedException> {
            this.paymentService.delete(id)
        }

        assertThat(exception.message, `is`(equalTo("Payment $id not found")))
        verify(this.paymentRepositoryPort, never()).delete(any())
        verify(this.paymentEventPublisherPort, never()).publish(any())
    }

    @Test
    fun `Should test the processed payment in delete`() {
        val id = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c")
        val payment = Payment()
        payment.pk = id
        payment.sk = EventType.PROCESSED_PAYMENT.name
        whenever(this.paymentRepositoryPort.findByPk(id.toString()))
            .thenReturn(payment)

        val exception = assertThrows<PaymentDeletionNotAllowedException> {
            this.paymentService.delete(id.toString())
        }

        assertThat(exception.message, `is`(equalTo("Payment processed $id can't be change")))
        verify(this.paymentRepositoryPort, never()).delete(eq(payment))
        verify(this.paymentEventPublisherPort, never()).publish(any())
    }

    @Test
    fun `Should test the deleted payment`() {
        val id = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c")
        val payment = Payment()
        payment.pk = id
        payment.sk = EventType.SCHEDULED_PAYMENT.name
        payment.date = LocalDate.now()
        whenever(this.paymentRepositoryPort.findByPk(id.toString()))
            .thenReturn(payment)

        this.paymentService.delete(id.toString())

        verify(this.paymentRepositoryPort, times(1)).delete(eq(payment))
        verify(this.paymentEventPublisherPort, times(1)).publish(this.argumentCaptor.capture())
        assertAll("Assert payment event", {
            assertThat(this.argumentCaptor.firstValue.id, `is`(equalTo(payment.pk)))
            assertThat(this.argumentCaptor.firstValue.eventType, `is`(equalTo(EventType.DELETED_PAYMENT.name)))
            assertThat(this.argumentCaptor.firstValue.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        })
    }

    @Test
    fun `Should not publish the event when the payment is not saved`() {
        val payment = Payment(pk = UUID.randomUUID(), sk = EventType.SCHEDULED_PAYMENT.name)
        whenever(this.paymentRepositoryPort.save(eq(payment)))
            .thenThrow(RuntimeException("dynamo is down"))

        val exception = assertThrows<RuntimeException> {
            this.paymentService.save(payment)
        }

        assertThat(exception.message, `is`(equalTo("dynamo is down")))
        verify(this.paymentEventPublisherPort, never()).publish(any())
    }

    @Test
    fun `Should publish the deleted event with the date of the deleted payment`() {
        val date = LocalDate.now().plusDays(3)
        val payment = Payment(
            pk = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c"),
            sk = EventType.SCHEDULED_PAYMENT.name,
            date = date,
            pixKeyCredit = "393"
        )
        whenever(this.paymentRepositoryPort.findByPk(payment.pk.toString()))
            .thenReturn(payment)

        this.paymentService.delete(payment.pk.toString())

        verify(this.paymentEventPublisherPort, times(1)).publish(this.argumentCaptor.capture())
        assertThat(
            this.argumentCaptor.firstValue,
            `is`(equalTo(PaymentEvent(payment.pk, EventType.DELETED_PAYMENT.name, date, "393")))
        )
    }
}
