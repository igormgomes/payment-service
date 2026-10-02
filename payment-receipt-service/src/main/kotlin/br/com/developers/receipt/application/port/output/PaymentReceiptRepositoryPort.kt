package br.com.developers.receipt.application.port.output

import br.com.developers.receipt.domain.PaymentReceipt

interface PaymentReceiptRepositoryPort {
    fun findByPk(id: String): PaymentReceipt?
    fun save(paymentReceipt: PaymentReceipt): PaymentReceipt
    fun update(paymentReceipt: PaymentReceipt): PaymentReceipt
}
