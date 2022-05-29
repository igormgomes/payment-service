package br.com.developers.receipt

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentReceiptRequest(
    val id: String?,
    @JsonProperty("event_type")
    val eventType: String?,
    @JsonProperty("date")
    val date: String?,
    @JsonProperty("pix_key_credit")
    val pixKeyCredit: String?,
)