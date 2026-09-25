package br.com.developers.payment.adapters.output.sns

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentEventRequest(
    @JsonProperty("id")
    val id: String?,
    @JsonProperty("event_type")
    val eventType: String?,
    @JsonProperty("date")
    val date: String?,
    @JsonProperty("pix_key_credit")
    val pixKeyCredit: String?,
)
