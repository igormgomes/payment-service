package br.com.developers.payment.adapters.input.rest

import br.com.developers.payment.domain.Payment
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

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

        return Payment.create(
            date = this.date,
            value = this.value,
            description = this.description,
            pixKeyCredit = this.creditRequest?.pixKey
        )
    }
}

data class CreditRequest(
    @field:NotBlank
    @JsonProperty("pix_key")
    val pixKey: String? = null
)
