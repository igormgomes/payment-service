package br.com.developers.payment

import br.com.developers.payment.converter.BigDecimalConverter
import br.com.developers.payment.converter.LocalDateConverter
import com.amazonaws.services.dynamodbv2.datamodeling.*
import org.springframework.data.annotation.Id
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@DynamoDBTable(tableName = "payment")
class Payment {
    @Id
    private var paymentKey: PaymentKey? = null

    @DynamoDBAttribute(attributeName = "date")
    @DynamoDBTypeConverted(converter = LocalDateConverter::class)
    var date: LocalDate? = null

    @DynamoDBAttribute(attributeName = "value")
    @DynamoDBTypeConverted(converter = BigDecimalConverter::class)
    var value: BigDecimal? = null

    @DynamoDBAttribute(attributeName = "description")
    var description: String? = null

    @DynamoDBAttribute(attributeName = "pix_key_credit")
    var pixKeyCredit: String? = null

    @DynamoDBAttribute(attributeName = "ttl")
    var ttl = Instant.now().plus(Duration.ofMinutes(60)).epochSecond

    @get:DynamoDBHashKey(attributeName = "pk")
    var pk: String
        get() = this.paymentKey?.pk.orEmpty()
        set(pk) {
            this.paymentKey = if(paymentKey == null) PaymentKey() else this.paymentKey
            this.paymentKey?.pk = pk
        }

    @get:DynamoDBRangeKey(attributeName = "sk")
    var sk: String?
        get() = this.paymentKey?.sk.orEmpty()
        set(sk) {
            this.paymentKey = if(paymentKey == null) PaymentKey() else this.paymentKey
            this.paymentKey?.sk = sk
        }
}

data class PaymentKey(
    @DynamoDBHashKey(attributeName = "pk")
    var pk: String? = null,

    @DynamoDBRangeKey(attributeName = "sk")
    var sk: String? = null
)

enum class EventType {
    PROCESSED_PAYMENT, SCHEDULED_PAYMENT
}