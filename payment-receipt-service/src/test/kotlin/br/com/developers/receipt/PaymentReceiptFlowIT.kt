package br.com.developers.receipt

import br.com.developers.receipt.adapters.input.sqs.PaymentReceiptRequest
import br.com.developers.receipt.adapters.input.sqs.PaymentReceiptSnsPayloadRequest
import br.com.developers.receipt.adapters.input.sqs.PaymentReceiptSnsRequest
import br.com.developers.receipt.application.port.output.PaymentReceiptRepositoryPort
import br.com.developers.receipt.domain.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
class PaymentReceiptFlowIT {

    companion object {

        @JvmStatic
        @Container
        private val localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.3"))
            .withServices(DYNAMODB, SQS)

        init {
            localStack.start()
            localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "payment-receipt")
            localStack.execInContainer(
                "awslocal", "dynamodb", "create-table",
                "--table-name", "payment_receipt",
                "--attribute-definitions", "AttributeName=pk,AttributeType=S",
                "--key-schema", "AttributeName=pk,KeyType=HASH",
                "--billing-mode", "PAY_PER_REQUEST"
            )
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.dynamodb.endpoint") { localStack.getEndpointOverride(DYNAMODB).toString() }
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.getEndpointOverride(SQS).toString() }
            registry.add("spring.cloud.aws.credentials.access-key") { "foo" }
            registry.add("spring.cloud.aws.credentials.secret-key") { "bar" }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
        }
    }

    @Autowired
    private lateinit var sqsTemplate: SqsTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var paymentReceiptRepositoryPort: PaymentReceiptRepositoryPort

    private fun sendEvent(id: String, eventType: String, pixKeyCredit: String = "123") {
        val payload = PaymentReceiptSnsPayloadRequest(
            payload = PaymentReceiptRequest(
                id = id,
                eventType = eventType,
                date = LocalDate.now(),
                pixKeyCredit = pixKeyCredit
            )
        )
        val json = this.objectMapper.writeValueAsString(payload)
        this.sqsTemplate.send("payment-receipt", PaymentReceiptSnsRequest(message = json))
    }

    @Test
    fun `Should persist a payment receipt end-to-end when a processed payment event is consumed`() {
        val id = UUID.randomUUID().toString()

        sendEvent(id, EventType.PROCESSED_PAYMENT.name)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val found = paymentReceiptRepositoryPort.findByPk(id)
                assertThat(found, `is`(notNullValue()))
                assertThat(found!!.status, `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
            }
    }

    @Test
    fun `Should move a payment receipt to deleted status end-to-end when a deleted payment event is consumed`() {
        val id = UUID.randomUUID().toString()
        sendEvent(id, EventType.PROCESSED_PAYMENT.name)
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted { assertThat(paymentReceiptRepositoryPort.findByPk(id), `is`(notNullValue())) }

        sendEvent(id, EventType.DELETED_PAYMENT.name)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val found = paymentReceiptRepositoryPort.findByPk(id)
                assertThat(found, `is`(notNullValue()))
                assertThat(found!!.status, `is`(equalTo(EventType.DELETED_PAYMENT.name)))
            }
    }

    @Test
    fun `Should stay consistent when the same event is reprocessed`() {
        val id = UUID.randomUUID().toString()

        sendEvent(id, EventType.PROCESSED_PAYMENT.name, pixKeyCredit = "same-key")
        sendEvent(id, EventType.PROCESSED_PAYMENT.name, pixKeyCredit = "same-key")

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val found = paymentReceiptRepositoryPort.findByPk(id)
                assertThat(found, `is`(notNullValue()))
                assertThat(found!!.status, `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
                assertThat(found.pixKeyCredit, `is`(equalTo("same-key")))
            }
    }

    @Test
    fun `Should keep consuming valid messages after receiving a malformed payload`() {
        this.sqsTemplate.send("payment-receipt", PaymentReceiptSnsRequest(message = "not-a-valid-json-payload"))

        val id = UUID.randomUUID().toString()
        sendEvent(id, EventType.PROCESSED_PAYMENT.name)

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val found = paymentReceiptRepositoryPort.findByPk(id)
                assertThat(found, `is`(notNullValue()))
                assertThat(found!!.status, `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
            }
    }
}
