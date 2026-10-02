package br.com.developers.receipt.adapters.input.sqs

import br.com.developers.config.MessageConverterConfiguration
import br.com.developers.infra.sqs.SqsConfiguration
import br.com.developers.receipt.application.port.input.SavePaymentReceiptUseCase
import br.com.developers.receipt.domain.PaymentReceipt
import br.com.developers.receipt.support.execAwsLocal
import tools.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.awspring.cloud.test.sqs.SqsTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("integration-test")
@SqsTest(PaymentReceiptConsumer::class)
@ImportAutoConfiguration(SqsConfiguration::class, MessageConverterConfiguration::class)
class PaymentReceiptConsumerIT {

    companion object {

        private val logger = LoggerFactory.getLogger(PaymentReceiptConsumerIT::class.java)

        @JvmStatic
        @Container
        private val localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices("sqs")

        init {
            localStack.start()
            localStack.execAwsLocal("sqs", "create-queue", "--queue-name", "payment-receipt")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.credentials.access-key") { localStack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localStack.secretKey }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
        }
    }

    @Autowired
    private lateinit var sqsTemplate: SqsTemplate
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var savePaymentReceiptUseCase: SavePaymentReceiptUseCase

    @Test
    fun `Should receive a message and save the payment`() {
        val paymentReceiptSnsPayloadRequest = PaymentReceiptSnsPayloadRequest(
            payload = PaymentReceiptRequest(
                id = "123e4567-e89b-12d3-a456-426614174000",
                eventType = "PROCESSED_PAYMENT",
                date = LocalDate.now(),
                pixKeyCredit = "123"
            )
        )
        val json = this.objectMapper.writeValueAsString(paymentReceiptSnsPayloadRequest)
        val paymentReceiptSnsRequest = PaymentReceiptSnsRequest(message = json)

        logger.info("Sending message to SQS: {}", paymentReceiptSnsRequest.message)
        this.sqsTemplate.send("payment-receipt", paymentReceiptSnsRequest)

        await()
            .atMost(Duration.ofSeconds(4))
            .untilAsserted { verify(this.savePaymentReceiptUseCase).save(any<PaymentReceipt>()) }
    }

    @Test
    fun `Should not save when the SNS message payload is malformed`() {
        val paymentReceiptSnsRequest = PaymentReceiptSnsRequest(message = "not-a-valid-json-payload")

        logger.info("Sending malformed message to SQS: {}", paymentReceiptSnsRequest.message)
        this.sqsTemplate.send("payment-receipt", paymentReceiptSnsRequest)

        await()
            .pollDelay(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(4))
            .untilAsserted { verify(this.savePaymentReceiptUseCase, never()).save(any()) }
    }
}
