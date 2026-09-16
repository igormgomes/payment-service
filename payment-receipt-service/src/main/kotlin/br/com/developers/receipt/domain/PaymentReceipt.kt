package br.com.developers.receipt.domain

import java.time.LocalDate
import java.util.UUID

data class PaymentReceipt(
    var pk: UUID? = null,
    var status: String? = null,
    var inclusionDate: LocalDate? = null,
    var paymentDate: LocalDate? = null,
    var pixKeyCredit: String? = null,
    var ttl: Long = ttlOf60Minutes()
)
