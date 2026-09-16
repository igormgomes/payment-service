package br.com.developers.receipt.adapters.input.rest.handler

import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResponse(
    @JsonProperty("errors")
    val errorMessageResponse: List<ErrorMessageResponse>? = null
)
