package br.com.developers.infra.dynamodb

import br.com.developers.payment.adapters.output.dynamodb.PaymentEntity
import io.awspring.cloud.dynamodb.DefaultDynamoDbTableNameResolver
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DynamoDbConfiguration {

    // Default class-name resolution would map PaymentEntity to "payment_entity", but the table is "payment".
    @Bean
    fun dynamoDbTableNameResolver(): DynamoDbTableNameResolver {
        val defaultResolver = DefaultDynamoDbTableNameResolver()

        return object : DynamoDbTableNameResolver {
            override fun <T> resolve(clazz: Class<T>): String =
                if (clazz == PaymentEntity::class.java) "payment" else defaultResolver.resolve(clazz)
        }
    }
}
