package br.com.developers.receipt

interface PaymentReceiptService {

    fun save(paymentReceipt: PaymentReceipt?)

    fun findById (id: String?): PaymentReceipt
}