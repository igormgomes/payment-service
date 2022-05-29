package br.com.developers.payment

import br.com.developers.event.PaymentEventPublisher
import br.com.developers.event.PaymentEventRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher
) : PaymentService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(payment: Payment?) {
        checkNotNull(payment)
        log.info("Saving payment $payment")

        val paymentSaved = this.paymentRepository.save(payment)
        log.info("Payment saved ${paymentSaved.pk}")

        PaymentEventRequest(
            id = paymentSaved.pk,
            eventType = paymentSaved.sk,
            pixKey = paymentSaved.pixKeyCredit
        ).apply {
            paymentEventPublisher.publish(this)
        }
    }

    override fun findAll(): List<Payment> {
        log.info("Finding all payments")

        return paymentRepository.findAll()
            .iterator()
            .asSequence()
            .toList()
    }

    override fun findById(id: String?): Payment {
        checkNotNull(id)
        log.info("Finding by $id")

        return paymentRepository.findByPk(id) ?: throw PaymentNotFoundException("Payment $id not found")
    }

    override fun delete(id: String?) {
        checkNotNull(id)
        log.info("Deleting payment $id")

        val payment =
            this.paymentRepository.findByPk(id) ?: throw PaymentDeletionNotAllowedException("Payment $id not found")
        payment.takeUnless { it.sk == EventType.PROCESSED_PAYMENT.name }
            ?: throw PaymentDeletionNotAllowedException("Payment processed $id can't be change")

        this.paymentRepository.delete(payment)
        log.info("Payments deleted $id")

        PaymentEventRequest(
            id = payment.pk,
            eventType = payment.sk,
            pixKey = payment.pixKeyCredit
        ).apply {
            paymentEventPublisher.publish(this)
        }
        log.info("Payment published ${payment.pk}")
    }
}