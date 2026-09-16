package br.com.developers.receipt.adapters.output.dynamodb

import br.com.developers.receipt.domain.EventType
import br.com.developers.receipt.domain.PaymentReceipt
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDate
import java.util.UUID

@DisplayName("Payment receipt mapper test")
class PaymentReceiptMapperTest {

    @Test
    fun `Should map domain PaymentReceipt to PaymentReceiptEntity preserving every field`() {
        val paymentReceipt = PaymentReceipt(
            pk = UUID.randomUUID(),
            status = EventType.PROCESSED_PAYMENT.name,
            inclusionDate = LocalDate.now(),
            paymentDate = LocalDate.now().plusDays(1),
            pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32",
            ttl = 123456789L
        )

        val entity = paymentReceipt.toEntity()

        assertAll("Assert entity fields", {
            assertThat(entity.pk, `is`(equalTo(paymentReceipt.pk)))
            assertThat(entity.status, `is`(equalTo(paymentReceipt.status)))
            assertThat(entity.inclusionDate, `is`(equalTo(paymentReceipt.inclusionDate)))
            assertThat(entity.paymentDate, `is`(equalTo(paymentReceipt.paymentDate)))
            assertThat(entity.pixKeyCredit, `is`(equalTo(paymentReceipt.pixKeyCredit)))
            assertThat(entity.ttl, `is`(equalTo(paymentReceipt.ttl)))
        })
    }

    @Test
    fun `Should map PaymentReceiptEntity to domain PaymentReceipt preserving every field`() {
        val entity = PaymentReceiptEntity(
            pk = UUID.randomUUID(),
            status = EventType.DELETED_PAYMENT.name,
            inclusionDate = LocalDate.now(),
            paymentDate = LocalDate.now().plusDays(2),
            pixKeyCredit = "123",
            ttl = 987654321L
        )

        val paymentReceipt = entity.toDomain()

        assertAll("Assert domain fields", {
            assertThat(paymentReceipt.pk, `is`(equalTo(entity.pk)))
            assertThat(paymentReceipt.status, `is`(equalTo(entity.status)))
            assertThat(paymentReceipt.inclusionDate, `is`(equalTo(entity.inclusionDate)))
            assertThat(paymentReceipt.paymentDate, `is`(equalTo(entity.paymentDate)))
            assertThat(paymentReceipt.pixKeyCredit, `is`(equalTo(entity.pixKeyCredit)))
            assertThat(paymentReceipt.ttl, `is`(equalTo(entity.ttl)))
        })
    }
}
