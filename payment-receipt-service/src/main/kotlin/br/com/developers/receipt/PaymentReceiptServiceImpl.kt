package br.com.developers.receipt

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class PaymentReceiptServiceImpl(
    private val paymentReceiptRepository: PaymentReceiptRepository
) :
    PaymentReceiptService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(paymentReceipt: PaymentReceipt?) {
        checkNotNull(paymentReceipt)
        checkNotNull(paymentReceipt.pk)
        log.info("Saving payment receipt $paymentReceipt")

        if (paymentReceipt.status == EventType.DELETED_PAYMENT.name) {
            paymentReceiptRepository.update(paymentReceipt)
            log.info("Payment receipt upddated ${paymentReceipt.pk}")
            return
        }

        val paymentReceiptSaved = this.paymentReceiptRepository.save(paymentReceipt)
        log.info("Payment receipt saved ${paymentReceiptSaved.pk}")
    }

    override fun findById(id: String?): PaymentReceipt {
        checkNotNull(id)
        log.info("Finding receipt by $id")

        return paymentReceiptRepository.findByPk(id) ?: throw PaymentReceiptNotFoundException("Payment $id not found")
    }
}