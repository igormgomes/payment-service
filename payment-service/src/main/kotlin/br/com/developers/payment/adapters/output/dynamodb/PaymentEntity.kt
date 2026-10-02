package br.com.developers.payment.adapters.output.dynamodb

import br.com.developers.payment.domain.ttlOf60Minutes
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DynamoDbBean
data class PaymentEntity(
    @get:DynamoDbPartitionKey
    var pk: UUID? = null,

    @get:DynamoDbSortKey
    var sk: String? = null,

    @get:DynamoDbAttribute(value = "date")
    var date: LocalDate? = null,

    @get:DynamoDbAttribute(value = "value")
    var value: BigDecimal? = null,

    @get:DynamoDbAttribute(value = "description")
    var description: String? = null,

    @get:DynamoDbAttribute(value = "pix_key_credit")
    var pixKeyCredit: String? = null,

    @get:DynamoDbAttribute(value = "ttl")
    var ttl: Long = ttlOf60Minutes()
)
