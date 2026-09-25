package br.com.developers.payment.adapters.output.dynamodb

import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.Payment
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("Payment mapper test")
class PaymentMapperTest {

    // A ttl different from the default proves the mapper copies it instead of generating a new one.
    private val payment = Payment(
        pk = UUID.fromString("8d369a41-a278-4390-9fc8-9cd32425bf4c"),
        sk = EventType.SCHEDULED_PAYMENT.name,
        date = LocalDate.of(2026, 10, 1),
        value = BigDecimal("10.50"),
        description = "rent",
        pixKeyCredit = "a@b.com",
        ttl = 12345L
    )

    @Test
    fun `Should map the domain to the entity with all fields`() {
        val entity = this.payment.toEntity()

        assertAll("Assert entity", {
            assertThat(entity.pk, `is`(equalTo(this.payment.pk)))
            assertThat(entity.sk, `is`(equalTo(this.payment.sk)))
            assertThat(entity.date, `is`(equalTo(this.payment.date)))
            assertThat(entity.value, `is`(equalTo(this.payment.value)))
            assertThat(entity.description, `is`(equalTo(this.payment.description)))
            assertThat(entity.pixKeyCredit, `is`(equalTo(this.payment.pixKeyCredit)))
            assertThat(entity.ttl, `is`(equalTo(12345L)))
        })
    }

    @Test
    fun `Should map the entity to the domain with all fields`() {
        val payment = this.payment.toEntity().toDomain()

        assertThat(payment, `is`(equalTo(this.payment)))
    }
}
