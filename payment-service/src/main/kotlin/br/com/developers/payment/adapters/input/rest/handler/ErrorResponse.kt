package br.com.developers.payment.adapters.input.rest.handler

import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResponse(
    @JsonProperty("errors")
    val errorMessageResponse: List<ErrorMessageResponse>? = null
)
