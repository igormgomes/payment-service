package br.com.developers.payment.adapters.output.dynamodb

import br.com.developers.payment.support.execAwsLocal
import br.com.developers.payment.domain.EventType
import br.com.developers.payment.domain.Payment
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.utility.DockerImageName
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS
import org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB
import org.testcontainers.containers.localstack.LocalStackContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
class PaymentDynamoDbAdapterIT {

    companion object {

        @JvmStatic
        @Container
        private val localStack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.3"))
                .withServices(DYNAMODB, SNS, SQS)

        init {
            localStack.start()
            localStack.execAwsLocal(
                "dynamodb", "create-table",
                "--table-name", "payment",
                "--attribute-definitions", "AttributeName=pk,AttributeType=S", "AttributeName=sk,AttributeType=S",
                "--key-schema", "AttributeName=pk,KeyType=HASH", "AttributeName=sk,KeyType=RANGE",
                "--billing-mode", "PAY_PER_REQUEST"
            )
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.dynamodb.endpoint") { localStack.getEndpointOverride(DYNAMODB).toString() }
            registry.add("spring.cloud.aws.sns.endpoint") { localStack.getEndpointOverride(SNS).toString() }
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.getEndpointOverride(SQS).toString() }
            registry.add("spring.cloud.aws.credentials.access-key") { "foo" }
            registry.add("spring.cloud.aws.credentials.secret-key") { "bar" }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
        }
    }

    @Autowired
    private lateinit var paymentDynamoDbAdapter: PaymentDynamoDbAdapter

    @Autowired
    private lateinit var dynamoDbClient: DynamoDbClient

    @Test
    fun `Should save and find payment by id`() {
        val payment = Payment().apply {
            pk = UUID.randomUUID()
            sk = EventType.PROCESSED_PAYMENT.name
            date = LocalDate.now()
            value = BigDecimal("50.00")
            description = "test payment"
            pixKeyCredit = "pix-key-123"
        }

        paymentDynamoDbAdapter.save(payment)
        val found = paymentDynamoDbAdapter.findByPk(payment.pk.toString())

        assertThat(found, `is`(notNullValue()))
        assertThat(found!!.pk, `is`(equalTo(payment.pk)))
        assertThat(found.sk, `is`(equalTo(payment.sk)))
        assertThat(found.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        // DynamoDB normalizes numbers (50.00 comes back as 50), so compare by value and not by scale
        assertThat(found.value!!.compareTo(payment.value), `is`(equalTo(0)))
    }

    @Test
    fun `Should return null when payment not found`() {
        val found = paymentDynamoDbAdapter.findByPk(UUID.randomUUID().toString())

        assertThat(found, `is`(nullValue()))
    }

    @Test
    fun `Should delete payment`() {
        val payment = Payment().apply {
            pk = UUID.randomUUID()
            sk = EventType.SCHEDULED_PAYMENT.name
            date = LocalDate.now().plusDays(1)
            value = BigDecimal("100.00")
            description = "test delete"
            pixKeyCredit = "pix-key-456"
        }

        paymentDynamoDbAdapter.save(payment)
        paymentDynamoDbAdapter.delete(payment)
        val found = paymentDynamoDbAdapter.findByPk(payment.pk.toString())

        assertThat(found, `is`(nullValue()))
    }

    @Test
    fun `Should store the payment in the payment table`() {
        val payment = Payment().apply {
            pk = UUID.randomUUID()
            sk = EventType.SCHEDULED_PAYMENT.name
            date = LocalDate.now().plusDays(1)
            value = BigDecimal("10.00")
            description = "test table"
            pixKeyCredit = "pix-key-789"
        }

        paymentDynamoDbAdapter.save(payment)

        // Without DynamoDbConfiguration, PaymentEntity would resolve to the "payment_entity" table.
        val item = dynamoDbClient.getItem(
            GetItemRequest.builder()
                .tableName("payment")
                .key(
                    mapOf(
                        "pk" to AttributeValue.builder().s(payment.pk.toString()).build(),
                        "sk" to AttributeValue.builder().s(payment.sk).build()
                    )
                )
                .build()
        ).item()
        assertThat(item["pix_key_credit"]?.s(), `is`(equalTo("pix-key-789")))
    }
}
