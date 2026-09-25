package br.com.developers.payment.adapters.input.rest

import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import br.com.developers.payment.domain.EventType
import java.math.BigDecimal
import java.time.LocalDate

class PaymentRequestTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun before () {
        val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `Should test the payment violations`() {
        val paymentRequest = PaymentRequest()

        val violations = this.validator.validate(paymentRequest)

        assertThat(violations.size, `is`(equalTo(3)))
    }

    @Test
    fun `Should not have violations when the payment is valid`() {
        val paymentRequest = PaymentRequest(LocalDate.now(), BigDecimal("0.01"), null, CreditRequest("a@b.com"))

        val violations = this.validator.validate(paymentRequest)

        assertThat(violations.size, `is`(equalTo(0)))
    }

    @Test
    fun `Should have a violation when the date is in the past`() {
        val paymentRequest = PaymentRequest(LocalDate.now().minusDays(1), BigDecimal.ONE, null, CreditRequest("a@b.com"))

        val violations = this.validator.validate(paymentRequest)

        assertThat(violations.map { it.propertyPath.toString() }, `is`(equalTo(listOf("date"))))
    }

    @Test
    fun `Should have a violation when the value is lower than 0_01`() {
        val paymentRequest = PaymentRequest(LocalDate.now(), BigDecimal("0.001"), null, CreditRequest("a@b.com"))

        val violations = this.validator.validate(paymentRequest)

        assertThat(violations.map { it.propertyPath.toString() }, `is`(equalTo(listOf("value"))))
    }

    @Test
    fun `Should convert the request to a scheduled payment when the date is in the future`() {
        val date = LocalDate.now().plusDays(1)
        val paymentRequest = PaymentRequest(date, BigDecimal("10.50"), "rent", CreditRequest("a@b.com"))

        val payment = paymentRequest.toPayment()

        assertAll("Assert payment", {
            assertThat(payment.sk, `is`(equalTo(EventType.SCHEDULED_PAYMENT.name)))
            assertThat(payment.date, `is`(equalTo(date)))
            assertThat(payment.value?.compareTo(BigDecimal("10.50")), `is`(equalTo(0)))
            assertThat(payment.description, `is`(equalTo("rent")))
            assertThat(payment.pixKeyCredit, `is`(equalTo("a@b.com")))
        })
    }

    @Test
    fun `Should fail to convert the request to a payment when the date is null`() {
        assertThrows<IllegalStateException> {
            PaymentRequest().toPayment()
        }
    }
}
