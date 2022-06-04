package br.com.developers.receipt

import br.com.developers.receipt.converter.LocalDateConverter
import com.amazonaws.services.dynamodbv2.datamodeling.*
import org.springframework.data.annotation.Id
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@DynamoDBTable(tableName = "payment_receipt")
class PaymentReceipt {

    @Id
    private var paymentReceiptKey: PaymentReceiptKey? = null

    @DynamoDBTypeConvertedEnum
    var eventType: EventType? = null

    @DynamoDBAttribute(attributeName = "inclusion_date")
    @DynamoDBTypeConverted(converter = LocalDateConverter::class)
    var inclusionDate: LocalDate? = null

    @DynamoDBAttribute(attributeName = "payment_date")
    @DynamoDBTypeConverted(converter = LocalDateConverter::class)
    var paymentDate: LocalDate? = null

    @DynamoDBAttribute(attributeName = "pix_key_credit")
    var pixKeyCredit: String? = null

    @DynamoDBAttribute(attributeName = "ttl")
    var ttl = Instant.now().plus(Duration.ofMinutes(60)).epochSecond

    @get:DynamoDBHashKey(attributeName = "pk")
    var pk: String
        get() = this.paymentReceiptKey?.pk.orEmpty()
        set(pk) {
            this.paymentReceiptKey = if(paymentReceiptKey == null) PaymentReceiptKey() else this.paymentReceiptKey
            this.paymentReceiptKey?.pk = pk
        }

    override fun toString(): String {
        return "PaymentReceipt(paymentReceiptKey=$paymentReceiptKey, eventType=$eventType')"
    }
}

enum class EventType {
    PROCESSED_PAYMENT, SCHEDULED_PAYMENT
}