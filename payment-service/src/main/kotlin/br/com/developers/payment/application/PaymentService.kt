package br.com.developers.payment.application

import br.com.developers.payment.application.port.input.DeletePaymentUseCase
import br.com.developers.payment.application.port.input.FindPaymentUseCase
import br.com.developers.payment.application.port.input.SavePaymentUseCase
import br.com.developers.payment.application.port.output.PaymentEventPublisherPort
import br.com.developers.payment.application.port.output.PaymentRepositoryPort
import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.Payment
import br.com.developers.payment.domain.PaymentDeletionNotAllowedException
import br.com.developers.payment.domain.PaymentEvent
import br.com.developers.payment.domain.PaymentNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PaymentService(
    private val paymentRepositoryPort: PaymentRepositoryPort,
    private val paymentEventPublisherPort: PaymentEventPublisherPort
) : SavePaymentUseCase, FindPaymentUseCase, DeletePaymentUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(payment: Payment) {
        log.info("Saving payment ${payment.pk} ${payment.sk}")

        val paymentSaved = this.paymentRepositoryPort.save(payment)
        log.info("Payment saved ${paymentSaved.pk}")

        PaymentEvent(
            id = checkNotNull(paymentSaved.pk),
            eventType = EventType.valueOf(checkNotNull(paymentSaved.sk)),
            date = checkNotNull(paymentSaved.date),
            pixKeyCredit = paymentSaved.pixKeyCredit
        ).apply {
            paymentEventPublisherPort.publish(this)
        }
    }

    override fun findById(id: String): Payment {
        log.info("Finding by $id")

        return paymentRepositoryPort.findByPk(id) ?: throw PaymentNotFoundException("Payment $id not found")
    }

    override fun delete(id: String) {
        log.info("Deleting payment $id")

        val payment =
            this.paymentRepositoryPort.findByPk(id) ?: throw PaymentDeletionNotAllowedException("Payment $id not found")
        payment.takeUnless { it.sk == EventType.PROCESSED_PAYMENT.name }
            ?: throw PaymentDeletionNotAllowedException("Payment processed $id can't be change")

        val event = PaymentEvent(
            id = checkNotNull(payment.pk),
            eventType = EventType.DELETED_PAYMENT,
            date = checkNotNull(payment.date),
            pixKeyCredit = payment.pixKeyCredit
        )

        this.paymentRepositoryPort.delete(payment)
        log.info("Payments deleted $id")

        paymentEventPublisherPort.publish(event)
        log.info("Payment published ${payment.pk}")
    }
}
