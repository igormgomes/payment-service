package br.com.developers.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PaymentServiceImpl(private val paymentRepository: PaymentRepository): PaymentService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save (payment: Payment?) {
        checkNotNull(payment)
        log.info("Saving payment $payment")

        val paymentSaved = this.paymentRepository.save(payment)

        log.info("Payment saved ${paymentSaved.pk}")
    }

    override fun findAll (): List<Payment> {
        log.info("Finding all payments")

        val payments = this.paymentRepository.findAll()
            .iterator()
            .asSequence()
            .toList()

        log.info("Payments found $payments")
        return payments
    }

    override fun delete(id: String?) {
        checkNotNull(id)
        log.info("Deleting payment $id")

        val payment = this.paymentRepository.findByPk(id) ?: throw PaymentDeletionNotAllowedException("Payment $id not found")
        payment.takeUnless { it.sk == EventType.PROCESSED_PAYMENT.name }  ?: throw PaymentDeletionNotAllowedException("Payment processed $id can't be change")

        this.paymentRepository.delete(payment)
        log.info("Payments deleted $id")
    }
}