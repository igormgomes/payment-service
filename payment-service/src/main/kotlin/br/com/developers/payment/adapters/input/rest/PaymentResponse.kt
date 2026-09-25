package br.com.developers.payment.adapters.input.rest

import br.com.developers.payment.domain.Payment
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

// Field names and order are the JSON contract of POST/GET /api/payment (fixed by PaymentControllerIT).
data class PaymentResponse(
    val pk: UUID?,
    val sk: String?,
    val date: LocalDate?,
    val value: BigDecimal?,
    val description: String?,
    val pixKeyCredit: String?,
    val ttl: Long
)

fun Payment.toResponse(): PaymentResponse = PaymentResponse(
    pk = this.pk,
    sk = this.sk,
    date = this.date,
    value = this.value,
    description = this.description,
    pixKeyCredit = this.pixKeyCredit,
    ttl = this.ttl
)
