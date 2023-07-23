package br.com.developers.receipt

import io.awspring.cloud.dynamodb.DynamoDbOperations
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

@Component
class PaymentReceiptRepository(private val dynamoDbOperations: DynamoDbOperations) {

    fun findByPk(id: String): PaymentReceipt? {
        val key = Key.builder()
            .partitionValue(id)
            .build()
        val queryConditional = QueryConditional.keyEqualTo(key)
        val queryEnhancedRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build()

        return this.dynamoDbOperations.query(queryEnhancedRequest, PaymentReceipt::class.java).items().firstOrNull()
    }

    fun save(payment: PaymentReceipt): PaymentReceipt {
        return this.dynamoDbOperations.save(payment)
    }

    fun update(payment: PaymentReceipt): PaymentReceipt {
        return this.dynamoDbOperations.update(payment)
    }
}