package br.com.developers.infra.dynamodb

import br.com.developers.payment.adapters.output.dynamodb.PaymentEntity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DynamoDb configuration test")
class DynamoDbConfigurationTest {

    private val tableNameResolver = DynamoDbConfiguration().dynamoDbTableNameResolver()

    @Test
    fun `Should resolve the payment entity to the payment table`() {
        val tableName = this.tableNameResolver.resolve(PaymentEntity::class.java)

        assertThat(tableName, `is`(equalTo("payment")))
    }

    @Test
    fun `Should resolve other classes with the default resolver`() {
        val tableName = this.tableNameResolver.resolve(String::class.java)

        assertThat(tableName, `is`(not(equalTo("payment"))))
    }
}
