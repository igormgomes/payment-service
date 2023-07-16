package br.com.developers.payment

interface PaymentService {

    fun save (payment: Payment?)
    fun findById (id: String?): Payment
    fun delete(id: String?)
}