package br.com.developers.event

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentEventRequest(
    val id: String?,
    @JsonProperty("event_type")
    val eventType: String?,
    @JsonProperty("date")
    val date: String?,
    @JsonProperty("pix_key_credit")
    val pixKeyCredit: String?,
)