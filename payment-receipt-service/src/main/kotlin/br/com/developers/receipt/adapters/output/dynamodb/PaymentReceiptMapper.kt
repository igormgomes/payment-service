package br.com.developers.receipt.adapters.output.dynamodb

import br.com.developers.receipt.domain.PaymentReceipt

fun PaymentReceipt.toEntity(): PaymentReceiptEntity = PaymentReceiptEntity(
    pk = this.pk,
    status = this.status,
    inclusionDate = this.inclusionDate,
    paymentDate = this.paymentDate,
    pixKeyCredit = this.pixKeyCredit,
    ttl = this.ttl
)

fun PaymentReceiptEntity.toDomain(): PaymentReceipt = PaymentReceipt(
    pk = this.pk,
    status = this.status,
    inclusionDate = this.inclusionDate,
    paymentDate = this.paymentDate,
    pixKeyCredit = this.pixKeyCredit,
    ttl = this.ttl
)
