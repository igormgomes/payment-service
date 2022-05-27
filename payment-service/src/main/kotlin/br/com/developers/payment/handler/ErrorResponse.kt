package br.com.developers.payment.handler

import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResponse(
    @JsonProperty("error_message")
    val errorMessage: List<ErrorMessage>? = null
)
