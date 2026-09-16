package br.com.developers.receipt.adapters.output.dynamodb

import br.com.developers.receipt.application.port.output.PaymentReceiptRepositoryPort
import br.com.developers.receipt.domain.PaymentReceipt
import io.awspring.cloud.dynamodb.DynamoDbOperations
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

@Component
class PaymentReceiptDynamoDbAdapter(
    private val dynamoDbOperations: DynamoDbOperations
) : PaymentReceiptRepositoryPort {

    override fun findByPk(id: String): PaymentReceipt? {
        val key = Key.builder()
            .partitionValue(id)
            .build()
        val queryConditional = QueryConditional.keyEqualTo(key)
        val queryEnhancedRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build()

        return this.dynamoDbOperations.query(queryEnhancedRequest, PaymentReceiptEntity::class.java)
            .items().firstOrNull()?.toDomain()
    }

    override fun save(paymentReceipt: PaymentReceipt): PaymentReceipt {
        return this.dynamoDbOperations.save(paymentReceipt.toEntity()).toDomain()
    }

    override fun update(paymentReceipt: PaymentReceipt): PaymentReceipt {
        return this.dynamoDbOperations.update(paymentReceipt.toEntity()).toDomain()
    }
}
