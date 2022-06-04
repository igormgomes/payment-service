package br.com.developers.payment

import br.com.developers.receipt.*
import br.com.developers.receipt.PaymentReceiptServiceImpl
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.time.LocalDate

@DisplayName("Payment receipt service test")
class PaymentReceiptServiceTest {

    private val paymentReceiptRepository: PaymentReceiptRepository = mock()

    private lateinit var paymentReceiptService: PaymentReceiptService

    @BeforeEach
    fun before() {
        this.paymentReceiptService = PaymentReceiptServiceImpl(this.paymentReceiptRepository)
    }

    @Test
    fun `Should test the invalid payment receipt`() {
        val exception = assertThrows<IllegalStateException> {
            this.paymentReceiptService.save(null)
        }

        assertThat(exception.message, `is`(equalTo("Required value was null.")))
    }

    @Test
    fun `Should test the saved payment receipt`() {
        val paymentReceipt = PaymentReceipt().apply {
            this.pk = "44c7516-075b-4b52-8e90-9bb2207ce41c"
            this.eventType = EventType.PROCESSED_PAYMENT
            this.inclusionDate = LocalDate.now()
            this.paymentDate = LocalDate.now()
            this.pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32"
        }
        whenever(this.paymentReceiptRepository.save(paymentReceipt))
            .thenReturn(paymentReceipt)

        this.paymentReceiptService.save(paymentReceipt)

        verify(this.paymentReceiptRepository, atLeastOnce()).save(eq(paymentReceipt))
    }
}