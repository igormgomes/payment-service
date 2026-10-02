package br.com.developers.payment.application.port.input

import br.com.developers.payment.domain.Payment

interface SavePaymentUseCase {
    fun save(payment: Payment)
}
