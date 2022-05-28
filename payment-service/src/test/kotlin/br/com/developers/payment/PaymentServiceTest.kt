package br.com.developers.payment

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("Payment service test")
@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mock()

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun before () {
        this.paymentService = PaymentServiceImpl(this.paymentRepository)
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
            this.pk = "44c7516-075b-4b52-8e90-9bb2207ce41d"
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
    }

    @Test
    fun `Should test the find all payments`() {
        val payment = Payment().apply {
            this.pk = "44c7516-075b-4b52-8e90-9bb2207ce41d"
            this.sk = EventType.PROCESSED_PAYMENT.name
            this.date = LocalDate.now()
            this.value = BigDecimal.ONE
            this.description = "test"
            this.pixKeyCredit = "393"
        }
        whenever(this.paymentRepository.findAll())
            .thenReturn(listOf(payment))

        val payments = this.paymentService.findAll()

        assertThat(payments.isEmpty(), `is`(equalTo(false)))

        assertAll("Assert payment data", {
            assertThat(payments[0].pk, `is`(equalTo(payment.pk)))
            assertThat(payments[0].sk, `is`(equalTo(payment.sk)))
        })
    }
}