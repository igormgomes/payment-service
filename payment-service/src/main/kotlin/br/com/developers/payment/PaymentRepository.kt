package br.com.developers.payment

import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
@EnableScan
interface PaymentRepository: CrudRepository<Payment, PaymentKey> {
    fun findByPk(id: String): Payment?
}