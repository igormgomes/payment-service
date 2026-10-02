package br.com.developers.payment.adapters.output.dynamodb

import br.com.developers.payment.application.port.output.PaymentRepositoryPort
import br.com.developers.payment.domain.Payment
import io.awspring.cloud.dynamodb.DynamoDbTemplate
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

@Component
class PaymentDynamoDbAdapter(private val dynamoDbTemplate: DynamoDbTemplate) : PaymentRepositoryPort {

    override fun findByPk(id: String): Payment? {
        val key = Key.builder()
            .partitionValue(id)
            .build()
        val queryConditional = QueryConditional.keyEqualTo(key)
        val queryEnhancedRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build()

        return this.dynamoDbTemplate.query(queryEnhancedRequest, PaymentEntity::class.java)
            .items().firstOrNull()?.toDomain()
    }

    override fun delete(payment: Payment) {
        this.dynamoDbTemplate.delete(payment.toEntity())
    }

    override fun save(payment: Payment): Payment {
        return this.dynamoDbTemplate.save(payment.toEntity()).toDomain()
    }
}
