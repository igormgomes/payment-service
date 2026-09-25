package br.com.developers.payment.application.port.input

interface DeletePaymentUseCase {
    fun delete(id: String)
}
