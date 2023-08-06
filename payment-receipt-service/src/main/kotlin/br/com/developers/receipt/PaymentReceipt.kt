package br.com.developers.receipt

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*

@DynamoDbBean
data class PaymentReceipt (
    @get:DynamoDbPartitionKey
    var pk: UUID? = null,

    @get:DynamoDbAttribute(value = "status")
    var status: String? = null,

    @get:DynamoDbAttribute(value = "inclusion_date")
    var inclusionDate: LocalDate? = null,

    @get:DynamoDbAttribute(value = "payment_date")
    var paymentDate: LocalDate? = null,

    @get:DynamoDbAttribute(value = "pix_key_credit")
    var pixKeyCredit: String? = null,

    @get:DynamoDbAttribute(value = "ttl")
    var ttl: Long = Instant.now().plus(Duration.ofMinutes(60)).epochSecond
)
