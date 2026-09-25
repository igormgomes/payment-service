package br.com.developers.payment.domain

import java.time.LocalDate
import java.util.UUID

data class PaymentEvent(
    val id: UUID,
    val eventType: EventType,
    val date: LocalDate,
    val pixKeyCredit: String?
)
