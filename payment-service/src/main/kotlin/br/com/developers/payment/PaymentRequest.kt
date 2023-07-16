package br.com.developers.payment

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PaymentRequest(
    @field:NotNull
    @field:FutureOrPresent
    val date: LocalDate? = null,
    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val value: BigDecimal? = null,
    val description: String? = null,
    @field:NotNull
    @JsonProperty("credit")
    val creditRequest: CreditRequest? = null
) {
    fun toPayment(): Payment {
        checkNotNull(this.date)

        val payment = Payment()
        payment.pk = UUID.randomUUID()
        payment.sk = if(this.date == LocalDate.now()) EventType.PROCESSED_PAYMENT.name else EventType.SCHEDULED_PAYMENT.name
        payment.date = this.date
        payment.value = this.value
        payment.description = this.description
        payment.pixKeyCredit = this.creditRequest?.pixKey
        return payment
    }
}

data class CreditRequest(
    @field:NotBlank
    @JsonProperty("pix_key")
    val pixKey: String? = null
)