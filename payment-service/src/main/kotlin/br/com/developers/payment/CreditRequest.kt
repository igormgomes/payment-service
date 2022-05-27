package br.com.developers.payment

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotBlank

data class CreditRequest(
    @field:NotBlank
    @JsonProperty("pix_key")
    val pixKey: String? = null
)