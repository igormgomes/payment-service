package br.com.developers.receipt.application.port.input

import br.com.developers.receipt.domain.PaymentReceipt

interface FindPaymentReceiptUseCase {
    fun findById(id: String): PaymentReceipt
}
