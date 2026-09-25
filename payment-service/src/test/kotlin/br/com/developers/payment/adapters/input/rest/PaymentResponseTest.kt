package br.com.developers.payment.adapters.input.rest

import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.Payment
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("Payment response test")
class PaymentResponseTest {

    @Test
    fun `Should map the payment to the response with all fields`() {
        val payment = Payment(
            pk = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c"),
            sk = EventType.SCHEDULED_PAYMENT.name,
            date = LocalDate.of(2026, 10, 1),
            value = BigDecimal("10.50"),
            description = "rent",
            pixKeyCredit = "a@b.com",
            ttl = 12345L
        )

        val response = payment.toResponse()

        assertAll("Assert response", {
            assertThat(response.pk, `is`(equalTo(payment.pk)))
            assertThat(response.sk, `is`(equalTo(payment.sk)))
            assertThat(response.date, `is`(equalTo(payment.date)))
            assertThat(response.value, `is`(equalTo(payment.value)))
            assertThat(response.description, `is`(equalTo(payment.description)))
            assertThat(response.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
            assertThat(response.ttl, `is`(equalTo(12345L)))
        })
    }

    @Test
    fun `Should map the payment without description to a response with null description`() {
        val payment = Payment.create(LocalDate.now(), BigDecimal.ONE, null, "a@b.com")

        val response = payment.toResponse()

        assertAll("Assert response without description", {
            assertThat(response.description, `is`(nullValue()))
            assertThat(response.pk, `is`(equalTo(payment.pk)))
        })
    }
}
