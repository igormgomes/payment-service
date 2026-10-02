package br.com.developers.payment.application.port.output

import br.com.developers.payment.domain.Payment

interface PaymentRepositoryPort {
    fun findByPk(id: String): Payment?
    fun save(payment: Payment): Payment
    fun delete(payment: Payment)
}
