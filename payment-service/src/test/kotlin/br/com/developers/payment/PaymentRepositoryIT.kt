package br.com.developers.payment

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.*
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
class PaymentRepositoryIT {

    companion object {

        @JvmStatic
        @Container
        private val localStack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.3"))
                .withClasspathResourceMapping("/localstack/", "/docker-entrypoint-initaws.d", BindMode.READ_ONLY)
                .withServices(DYNAMODB, SNS, SQS)
                .waitingFor(Wait.forLogMessage(".*Initialized\\.\n", 1))

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
    private lateinit var paymentRepository: PaymentRepository

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

        paymentRepository.save(payment)
        val found = paymentRepository.findByPk(payment.pk.toString())

        assertThat(found, `is`(notNullValue()))
        assertThat(found!!.pk, `is`(equalTo(payment.pk)))
        assertThat(found.sk, `is`(equalTo(payment.sk)))
        assertThat(found.pixKeyCredit, `is`(equalTo(payment.pixKeyCredit)))
        assertThat(found.value, `is`(equalTo(payment.value)))
    }

    @Test
    fun `Should return null when payment not found`() {
        val found = paymentRepository.findByPk(UUID.randomUUID().toString())

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

        paymentRepository.save(payment)
        paymentRepository.delete(payment)
        val found = paymentRepository.findByPk(payment.pk.toString())

        assertThat(found, `is`(nullValue()))
    }
}
