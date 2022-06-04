package br.com.developers.receipt

import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
@EnableScan
interface PaymentReceiptRepository: CrudRepository<PaymentReceipt, PaymentReceiptKey> {

    fun findByPk(id: String): PaymentReceipt?
}