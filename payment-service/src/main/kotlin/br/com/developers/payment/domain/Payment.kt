package br.com.developers.payment.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class Payment(
    var pk: UUID? = null,
    var sk: String? = null,
    var date: LocalDate? = null,
    var value: BigDecimal? = null,
    var description: String? = null,
    var pixKeyCredit: String? = null,
    var ttl: Long = ttlOf60Minutes()
) {

    companion object {

        /**
         * Creates a new payment with a generated `pk`. The `sk` is `PROCESSED_PAYMENT` when the payment date is
         * today, otherwise `SCHEDULED_PAYMENT`.
         */
        fun create(date: LocalDate, value: BigDecimal?, description: String?, pixKeyCredit: String?): Payment =
            Payment(
                pk = UUID.randomUUID(),
                sk = if (date == LocalDate.now()) EventType.PROCESSED_PAYMENT.name else EventType.SCHEDULED_PAYMENT.name,
                date = date,
                value = value,
                description = description,
                pixKeyCredit = pixKeyCredit
            )
    }

    override fun toString(): String {
        return "Payment{" +
                "pk=" + pk +
                ", sk='" + sk + '\'' +
                ", date=" + date +
                ", value=" + value +
                ", description='" + description + '\'' +
                ", ttl=" + ttl +
                '}'
    }
}
