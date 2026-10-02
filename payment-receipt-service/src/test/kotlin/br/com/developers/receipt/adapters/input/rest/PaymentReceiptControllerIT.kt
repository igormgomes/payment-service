package br.com.developers.receipt.adapters.input.rest

import br.com.developers.receipt.application.port.output.PaymentReceiptRepositoryPort
import br.com.developers.receipt.domain.EventType
import br.com.developers.receipt.domain.PaymentReceipt
import br.com.developers.receipt.support.execAwsLocal
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.util.UUID

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("integration-test")
class PaymentReceiptControllerIT {

    companion object {

        @JvmStatic
        @Container
        private val localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices("dynamodb", "sqs")

        init {
            localStack.start()
            localStack.execAwsLocal("sqs", "create-queue", "--queue-name", "payment-receipt")
            localStack.execAwsLocal(
                "dynamodb", "create-table",
                "--table-name", "payment_receipt",
                "--attribute-definitions", "AttributeName=pk,AttributeType=S",
                "--key-schema", "AttributeName=pk,KeyType=HASH",
                "--billing-mode", "PAY_PER_REQUEST"
            )
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.dynamodb.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.credentials.access-key") { localStack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localStack.secretKey }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
        }
    }

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var paymentReceiptRepositoryPort: PaymentReceiptRepositoryPort

    @Test
    fun `Should return 200 with the payment receipt when it exists`() {
        val paymentReceipt = PaymentReceipt(
            pk = UUID.randomUUID(),
            status = EventType.PROCESSED_PAYMENT.name,
            inclusionDate = LocalDate.now(),
            paymentDate = LocalDate.now(),
            pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32"
        )
        paymentReceiptRepositoryPort.save(paymentReceipt)

        val response = restTemplate.getForEntity(
            "/api/payment-receipt/${paymentReceipt.pk}",
            Map::class.java
        )

        assertThat(response.statusCode, `is`(equalTo(HttpStatus.OK)))
        assertThat(response.body?.get("status"), `is`(equalTo(EventType.PROCESSED_PAYMENT.name)))
        assertThat(response.body?.get("pix_key_credit"), `is`(equalTo(paymentReceipt.pixKeyCredit)))
    }

    @Test
    fun `Should return 404 with the error contract when the payment receipt does not exist`() {
        val id = UUID.randomUUID()

        val response = restTemplate.getForEntity("/api/payment-receipt/$id", Map::class.java)

        assertThat(response.statusCode, `is`(equalTo(HttpStatus.NOT_FOUND)))
        val errors = response.body?.get("errors") as List<*>
        val firstError = errors.first() as Map<*, *>
        assertThat(firstError["message"], `is`(equalTo("Payment $id not found")))
    }
}
