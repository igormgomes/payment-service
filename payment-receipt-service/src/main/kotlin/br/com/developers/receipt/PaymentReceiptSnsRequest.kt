package br.com.developers.receipt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.*
import javax.validation.constraints.NotNull

data class PaymentReceiptSnsRequest(
    @field:NotNull
    @JsonProperty("Message")
    val message: String?,
    @JsonProperty("MessageId")
    val messageId: String?
)

data class PaymentReceiptRequest(
    val id: String?,
    @JsonProperty("event_type")
    val eventType: String?,
    @JsonProperty("date")
    val date: LocalDate?,
    @JsonProperty("pix_key_credit")
    val pixKeyCredit: String?,
) {

    fun toPaymentReceipt(): PaymentReceipt {
        val paymentReceipt = PaymentReceipt()
        paymentReceipt.pk = this.id ?: UUID.randomUUID().toString()
        paymentReceipt.eventType = this.eventType?.let { EventType.valueOf(it) }
        paymentReceipt.inclusionDate = LocalDate.now()
        paymentReceipt.paymentDate = this.date
        paymentReceipt.pixKeyCredit = this.pixKeyCredit
        return paymentReceipt
    }
}