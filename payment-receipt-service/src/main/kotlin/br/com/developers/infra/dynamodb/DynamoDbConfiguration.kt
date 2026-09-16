package br.com.developers.infra.dynamodb

import br.com.developers.receipt.adapters.output.dynamodb.PaymentReceiptEntity
import io.awspring.cloud.dynamodb.DefaultDynamoDbTableNameResolver
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DynamoDbConfiguration {

    // Default class-name resolution would map PaymentReceiptEntity to "payment_receipt_entity", but the CDK table is "payment_receipt".
    @Bean
    fun dynamoDbTableNameResolver(): DynamoDbTableNameResolver {
        val defaultResolver = DefaultDynamoDbTableNameResolver()

        return object : DynamoDbTableNameResolver {
            override fun <T> resolve(clazz: Class<T>): String =
                if (clazz == PaymentReceiptEntity::class.java) "payment_receipt" else defaultResolver.resolve(clazz)
        }
    }
}
