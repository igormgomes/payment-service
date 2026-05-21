package br.com.developers.receipt

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

@DisplayName("TtlUtils test")
class TtlUtilsTest {

    @Test
    fun `Should return epoch second 60 minutes from now`() {
        val now = Instant.now()
        val ttl = ttlOf60Minutes()

        assertThat(ttl, greaterThanOrEqualTo(now.plus(Duration.ofMinutes(59)).epochSecond))
        assertThat(ttl, lessThanOrEqualTo(now.plus(Duration.ofMinutes(61)).epochSecond))
    }

    @Test
    fun `Should set PaymentReceipt ttl to 60 minutes from now`() {
        val now = Instant.now()
        val receipt = PaymentReceipt()

        assertThat(receipt.ttl, greaterThanOrEqualTo(now.plus(Duration.ofMinutes(59)).epochSecond))
        assertThat(receipt.ttl, lessThanOrEqualTo(now.plus(Duration.ofMinutes(61)).epochSecond))
    }
}
