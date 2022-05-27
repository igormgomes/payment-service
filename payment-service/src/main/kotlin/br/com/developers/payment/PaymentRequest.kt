package br.com.developers.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotNull

data class PaymentRequest(
    @field:NotNull
    val date: LocalDate? = null,
    @field:NotNull
    @field:DecimalMin(value = "0.01")
    val value: BigDecimal? = null,
    val description: String? = null,
    @field:NotNull
    @JsonProperty("credit_request")
    val creditRequest: CreditRequest? = null
) {
    fun toPayment(): Payment {
        val payment = Payment()
        payment.sk = EventType.PROCESSED_PAYMENT.name
        payment.date = this.date?.toString()
        payment.value = this.value?.toString()
        payment.description = this.description
        payment.pixKeyCredit = this.creditRequest?.pixKey
        payment.ttl = Instant.now().plus(Duration.ofMinutes(60)).epochSecond
        return payment
    }
}