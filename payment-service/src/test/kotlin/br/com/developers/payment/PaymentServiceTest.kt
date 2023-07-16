package br.com.developers.payment

import br.com.developers.event.PaymentEventPublisher
import br.com.developers.event.PaymentEventRequest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@DisplayName("Payment service test")
@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mock()
    private val paymentEventPublisher: PaymentEventPublisher = mock()
    private val argumentCaptor = ArgumentCaptor.forClass(PaymentEventRequest::class.java)

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun before () {
        this.paymentService = PaymentServiceImpl(this.paymentRepository, this.paymentEventPublisher)
    }

    @Test
    fun `Should test the invalid payment`() {
        val exception = assertThrows<IllegalStateException> {
            this.paymentService.save(null)
        }

        assertThat(exception.message, `is`(equalTo("Required value was null.")))
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
        whenever(this.paymentRepository.save(eq(payment)))
            .thenReturn(payment)

        this.paymentService.save(payment)

        verify(this.paymentRepository, atLeastOnce()).save(eq(payment))
        verify(this.paymentEventPublisher, atLeastOnce()).publish(this.argumentCaptor.capture())
        assertAll("Assert payment request event", {
            assertThat(this.argumentCaptor.value.id, `is`(equalTo(payment.pk.toString())))
            assertThat(this.argumentCaptor.value.eventType, `is`(equalTo(payment.sk)))
            assertThat(this.argumentCaptor.value.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        })
    }

    @Test
    fun `Should test the invalid id in find by id`() {
        val exception = assertThrows<IllegalStateException> {
            this.paymentService.findById(null)
        }

        assertThat(exception.message, `is`(equalTo("Required value was null.")))
    }

    @Test
    fun `Should test the not found payment in find by id`() {
        val id = "8d369a41-a278-4390-9fc8-9cd32425bf4c"
        whenever(this.paymentRepository.findByPk(id))
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
        whenever(this.paymentRepository.findByPk(id.toString()))
            .thenReturn(paymentMock)

        val payment = this.paymentService.findById(id.toString())

        assertThat(payment.pk, `is`(equalTo(paymentMock.pk)))
    }

    @Test
    fun `Should test the invalid id in delete`() {
        val exception = assertThrows<IllegalStateException> {
            this.paymentService.delete(null)
        }

        assertThat(exception.message, `is`(equalTo("Required value was null.")))
    }

    @Test
    fun `Should test the not found payment in delete`() {
        val id = "8d369a41-a278-4390-9fc8-9cd32425bf4c"
        whenever(this.paymentRepository.findByPk(id))
            .thenReturn(null)

        val exception = assertThrows<PaymentDeletionNotAllowedException> {
            this.paymentService.delete(id)
        }

        assertThat(exception.message, `is`(equalTo("Payment $id not found")))
        verify(this.paymentRepository, never()).delete(any())
    }

    @Test
    fun `Should test the processed payment in delete`() {
        val id = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c")
        val payment = Payment()
        payment.pk = id
        payment.sk = EventType.PROCESSED_PAYMENT.name
        whenever(this.paymentRepository.findByPk(id.toString()))
            .thenReturn(payment)

        val exception = assertThrows<PaymentDeletionNotAllowedException> {
            this.paymentService.delete(id.toString())
        }

        assertThat(exception.message, `is`(equalTo("Payment processed $id can't be change")))
        verify(this.paymentRepository, never()).delete(eq(payment))
        verify(this.paymentEventPublisher, never()).publish(this.argumentCaptor.capture())
    }

    @Test
    fun `Should test the deleted payment`() {
        val id = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c")
        val payment = Payment()
        payment.pk = id
        payment.sk = EventType.SCHEDULED_PAYMENT.name
        payment.date = LocalDate.now()
        whenever(this.paymentRepository.findByPk(id.toString()))
            .thenReturn(payment)

        this.paymentService.delete(id.toString())

        verify(this.paymentRepository, atLeastOnce()).delete(eq(payment))
        verify(this.paymentEventPublisher, atLeastOnce()).publish(this.argumentCaptor.capture())
        assertAll("Assert payment request event", {
            assertThat(this.argumentCaptor.value.id, `is`(equalTo(payment.pk.toString())))
            assertThat(this.argumentCaptor.value.eventType, `is`(equalTo(payment.sk)))
            assertThat(this.argumentCaptor.value.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        })
    }
}