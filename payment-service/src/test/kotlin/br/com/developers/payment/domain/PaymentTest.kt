package br.com.developers.payment.domain

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("Payment test")
class PaymentTest {

    @Test
    fun `Should create a processed payment when the date is today`() {
        val today = LocalDate.now()

        val payment = Payment.create(today, BigDecimal("10.50"), "rent", "a@b.com")

        assertAll("Assert processed payment", {
            assertThat(payment.pk, `is`(notNullValue()))
            assertThat(payment.sk, `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
            assertThat(payment.date, `is`(equalTo(today)))
            assertThat(payment.value, `is`(equalTo(BigDecimal("10.50"))))
            assertThat(payment.description, `is`(equalTo("rent")))
            assertThat(payment.pixKeyCredit, `is`(equalTo("a@b.com")))
        })
    }

    @Test
    fun `Should create a scheduled payment when the date is in the future`() {
        val payment = Payment.create(LocalDate.now().plusDays(1), BigDecimal.ONE, null, "a@b.com")

        assertAll("Assert scheduled payment", {
            assertThat(payment.sk, `is`(equalTo(EventType.SCHEDULED_PAYMENT.name)))
            assertThat(payment.description, `is`(equalTo(null)))
        })
    }

    @Test
    fun `Should generate a new pk for each created payment`() {
        val date = LocalDate.now()

        val first = Payment.create(date, BigDecimal.ONE, "a", "k")
        val second = Payment.create(date, BigDecimal.ONE, "a", "k")

        assertThat(first.pk, `is`(not(equalTo(second.pk))))
    }

    @Test
    fun `Should not expose the pix key in the string representation`() {
        val payment = Payment.create(LocalDate.now(), BigDecimal.ONE, "rent", "secret-pix-key@b.com")

        val result = payment.toString()

        assertAll("Assert string representation", {
            assertThat(result, `is`(not(containsString("secret-pix-key@b.com"))))
            assertThat(result, `is`(not(containsString("pixKeyCredit"))))
            assertThat(result, `is`(containsString(payment.pk.toString())))
        })
    }
}
