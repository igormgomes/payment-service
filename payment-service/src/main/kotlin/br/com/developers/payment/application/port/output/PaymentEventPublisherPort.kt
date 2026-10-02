package br.com.developers.payment.application.port.output

import br.com.developers.payment.domain.PaymentEvent

interface PaymentEventPublisherPort {
    fun publish(event: PaymentEvent)
}
