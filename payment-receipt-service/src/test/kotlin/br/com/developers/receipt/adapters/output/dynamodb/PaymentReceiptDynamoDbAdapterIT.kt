package br.com.developers.receipt.adapters.output.dynamodb

import br.com.developers.receipt.application.port.output.PaymentReceiptRepositoryPort
import br.com.developers.receipt.domain.EventType
import br.com.developers.receipt.domain.PaymentReceipt
import br.com.developers.receipt.support.execAwsLocal
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
class PaymentReceiptDynamoDbAdapterIT {

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
    private lateinit var paymentReceiptRepositoryPort: PaymentReceiptRepositoryPort

    @Test
    fun `Should save and find a payment receipt by id`() {
        val paymentReceipt = PaymentReceipt(
            pk = UUID.randomUUID(),
            status = EventType.PROCESSED_PAYMENT.name,
            inclusionDate = LocalDate.now(),
            paymentDate = LocalDate.now(),
            pixKeyCredit = "cce7b651-3698-4ac7-a9d4-04980d56df32"
        )

        paymentReceiptRepositoryPort.save(paymentReceipt)
        val found = paymentReceiptRepositoryPort.findByPk(paymentReceipt.pk.toString())

        assertThat(found, `is`(notNullValue()))
        assertThat(found!!.pk, `is`(equalTo(paymentReceipt.pk)))
        assertThat(found.status, `is`(equalTo(paymentReceipt.status)))
        assertThat(found.inclusionDate, `is`(equalTo(paymentReceipt.inclusionDate)))
        assertThat(found.paymentDate, `is`(equalTo(paymentReceipt.paymentDate)))
        assertThat(found.pixKeyCredit, `is`(equalTo(paymentReceipt.pixKeyCredit)))
        assertThat(found.ttl, `is`(equalTo(paymentReceipt.ttl)))
    }

    @Test
    fun `Should return null when payment receipt is not found`() {
        val found = paymentReceiptRepositoryPort.findByPk(UUID.randomUUID().toString())

        assertThat(found, `is`(nullValue()))
    }

    @Test
    fun `Should update an existing payment receipt`() {
        val paymentReceipt = PaymentReceipt(
            pk = UUID.randomUUID(),
            status = EventType.PROCESSED_PAYMENT.name,
            inclusionDate = LocalDate.now(),
            paymentDate = LocalDate.now(),
            pixKeyCredit = "123"
        )
        paymentReceiptRepositoryPort.save(paymentReceipt)

        val updated = paymentReceipt.copy(status = EventType.DELETED_PAYMENT.name)
        paymentReceiptRepositoryPort.update(updated)
        val found = paymentReceiptRepositoryPort.findByPk(paymentReceipt.pk.toString())

        assertThat(found, `is`(notNullValue()))
        assertThat(found!!.status, `is`(equalTo(EventType.DELETED_PAYMENT.name)))
    }
}
