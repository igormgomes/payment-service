package br.com.developers.payment
import io.awspring.cloud.dynamodb.DynamoDbTemplate
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

@Component
class PaymentRepository(private val dynamoDbTemplate: DynamoDbTemplate) {
    fun findByPk(id: String): Payment? {
        val key = Key.builder()
            .partitionValue(id)
            .build()
        val queryConditional = QueryConditional.keyEqualTo(key)
        val queryEnhancedRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build()

        return this.dynamoDbTemplate.query(queryEnhancedRequest, Payment::class.java).items().firstOrNull()
    }

    fun delete(payment: Payment) {
        this.dynamoDbTemplate.delete(payment)
    }

    fun save(payment: Payment): Payment {
        return this.dynamoDbTemplate.save(payment)
    }
}