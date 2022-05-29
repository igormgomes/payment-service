package br.com.developers.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.FutureOrPresent
import javax.validation.constraints.NotNull

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
        payment.pk = UUID.randomUUID().toString()
        payment.sk = if(this.date == LocalDate.now()) EventType.PROCESSED_PAYMENT.name else EventType.SCHEDULED_PAYMENT.name
        payment.date = this.date
        payment.value = this.value
        payment.description = this.description
        payment.pixKeyCredit = this.creditRequest?.pixKey
        return payment
    }
}