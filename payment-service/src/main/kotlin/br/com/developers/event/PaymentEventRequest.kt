package br.com.developers.event

data class PaymentEventRequest(
    val id: String?,
    val eventType: String?,
    val pixKey: String?,
)