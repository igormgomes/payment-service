package br.com.developers.payment

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*

@DynamoDbBean
data class Payment (
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
    var ttl: Long = Instant.now().plus(Duration.ofMinutes(60)).epochSecond
) {
    override fun toString(): String {
        return "Payment{" +
                "pk=" + pk +
                ", sk='" + sk + '\'' +
                ", date=" + date +
                ", value=" + value +
                ", description='" + description + '\'' +
                ", pixKeyCredit='" + pixKeyCredit + '\'' +
                ", ttl=" + ttl +
                '}'
    }
}
