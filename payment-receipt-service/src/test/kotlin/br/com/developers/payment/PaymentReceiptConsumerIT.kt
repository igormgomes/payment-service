package br.com.developers.payment

import br.com.developers.receipt.PaymentReceiptConsumer
import br.com.developers.receipt.PaymentReceiptService
import br.com.developers.receipt.PaymentReceiptSnsRequest
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.awspring.cloud.test.sqs.SqsTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.slf4j.LoggerFactory

@Testcontainers
@SqsTest(PaymentReceiptConsumer::class)
@TestConfiguration(proxyBeanMethods = false)
@ActiveProfiles("test")
class PaymentReceiptConsumerIT {

    companion object {

        private val logger = LoggerFactory.getLogger(PaymentReceiptConsumerIT::class.java)

        @JvmStatic
        @Container
        var localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.3"))
            .withClasspathResourceMapping(
                "/localstack/", "/docker-entrypoint-initaws.d", BindMode.READ_ONLY
            )
            .withServices(SQS)
            .waitingFor(Wait.forLogMessage(".*Initialized\\.\n", 1))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.getEndpointOverride(SQS).toString() }
            registry.add("spring.cloud.aws.credentials.access-key") { "foo" }
            registry.add("spring.cloud.aws.credentials.secret-key") { "bar" }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
            registry.add("order-queue-name") { "payment-receipt" }
        }
    }

    @Autowired
    private lateinit var sqsTemplate: SqsTemplate

    @MockBean
    private lateinit var paymentReceiptService: PaymentReceiptService

    @Test
    fun `Should test the valid payment receipt request`() {
        val paymentReceiptSnsRequest = PaymentReceiptSnsRequest(message = "Test message")

        // Log a mensagem antes de enviar
        logger.info("Sending message to SQS: {}", paymentReceiptSnsRequest.message)
        try {
            sqsTemplate.send("payment-receipt", paymentReceiptSnsRequest)
            logger.info("Message sent successfully.")
        } catch (e: Exception) {
            logger.error("Failed to send message to SQS", e)
        }
    }
}