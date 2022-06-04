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
        log.info("Saving payment receipt $paymentReceipt")

        val paymentReceiptSaved = this.paymentReceiptRepository.save(paymentReceipt)
        log.info("Payment receipt saved ${paymentReceiptSaved.pk}")
    }
}