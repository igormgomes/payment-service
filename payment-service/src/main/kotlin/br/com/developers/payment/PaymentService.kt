package br.com.developers.payment

interface PaymentService {

    fun save (payment: Payment?)

    fun findAll (): List<Payment>
    fun findById (id: String?): Payment
    fun delete(id: String?)
}