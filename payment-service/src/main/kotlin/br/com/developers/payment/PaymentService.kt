package br.com.developers.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentService(private val paymentRepository: PaymentRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun save (payment: Payment?) {
        log.info("Saving payment $payment")
        checkNotNull(payment)

        val paymentSaved = this.paymentRepository.save(payment)
        log.info("Payment saved $paymentSaved")
    }
}