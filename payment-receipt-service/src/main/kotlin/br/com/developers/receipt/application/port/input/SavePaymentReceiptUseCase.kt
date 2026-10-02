package br.com.developers.receipt.application.port.input

import br.com.developers.receipt.domain.PaymentReceipt

interface SavePaymentReceiptUseCase {
    fun save(paymentReceipt: PaymentReceipt)
}
