package br.com.developers.receipt

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class PaymentReceiptSnsRequest(
    @field:NotNull
    @JsonProperty("Message")
    val message: String?,
)

data class PaymentReceiptSnsPayloadRequest(
    val payload: PaymentReceiptRequest,
)

data class PaymentReceiptRequest(
    val id: String?,
    @JsonProperty("event_type")
    val eventType: String?,
    val date: LocalDate?,
    @JsonProperty("pix_key_credit")
    val pixKeyCredit: String?,
) {

    fun toPaymentReceipt(): PaymentReceipt {
        val paymentReceipt = PaymentReceipt()
        paymentReceipt.pk = UUID.fromString(this.id)
        paymentReceipt.status = this.eventType?.let { EventType.valueOf(it).name }
        paymentReceipt.inclusionDate = LocalDate.now()
        paymentReceipt.paymentDate = this.date
        paymentReceipt.pixKeyCredit = this.pixKeyCredit
        return paymentReceipt
    }
}