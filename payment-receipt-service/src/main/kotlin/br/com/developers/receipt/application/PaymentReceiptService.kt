package br.com.developers.receipt.application

import br.com.developers.receipt.application.port.input.FindPaymentReceiptUseCase
import br.com.developers.receipt.application.port.input.SavePaymentReceiptUseCase
import br.com.developers.receipt.application.port.output.PaymentReceiptRepositoryPort
import br.com.developers.receipt.domain.EventType
import br.com.developers.receipt.domain.PaymentReceipt
import br.com.developers.receipt.domain.PaymentReceiptNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PaymentReceiptService(
    private val paymentReceiptRepositoryPort: PaymentReceiptRepositoryPort
) : SavePaymentReceiptUseCase, FindPaymentReceiptUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(paymentReceipt: PaymentReceipt) {
        checkNotNull(paymentReceipt.pk)
        log.info("Saving payment receipt $paymentReceipt")

        if (paymentReceipt.status == EventType.DELETED_PAYMENT.name) {
            paymentReceiptRepositoryPort.update(paymentReceipt)
            log.info("Payment receipt upddated ${paymentReceipt.pk}")
            return
        }

        val paymentReceiptSaved = this.paymentReceiptRepositoryPort.save(paymentReceipt)
        log.info("Payment receipt saved ${paymentReceiptSaved.pk}")
    }

    override fun findById(id: String): PaymentReceipt {
        log.info("Finding receipt by $id")

        return paymentReceiptRepositoryPort.findByPk(id) ?: throw PaymentReceiptNotFoundException("Payment $id not found")
    }
}
