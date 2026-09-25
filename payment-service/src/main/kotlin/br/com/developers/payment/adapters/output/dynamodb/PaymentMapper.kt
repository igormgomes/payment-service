package br.com.developers.payment.adapters.output.dynamodb

import br.com.developers.payment.domain.Payment

fun Payment.toEntity(): PaymentEntity = PaymentEntity(
    pk = this.pk,
    sk = this.sk,
    date = this.date,
    value = this.value,
    description = this.description,
    pixKeyCredit = this.pixKeyCredit,
    ttl = this.ttl
)

fun PaymentEntity.toDomain(): Payment = Payment(
    pk = this.pk,
    sk = this.sk,
    date = this.date,
    value = this.value,
    description = this.description,
    pixKeyCredit = this.pixKeyCredit,
    ttl = this.ttl
)
