package br.com.developers.payment

import br.com.developers.payment.support.execAwsLocal
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Characterization test of the REST contract. It only goes through HTTP, so it must keep passing, unchanged,
 * through the hexagonal refactoring. Requires Docker; run with `./mvnw test -Dtest=PaymentControllerIT`.
 */
@DisplayName("Payment controller integration test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class PaymentControllerIT {

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
            localStack.execAwsLocal("sns", "create-topic", "--name", "payment-event")
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
    private lateinit var restTemplate: TestRestTemplate

    private val objectMapper = ObjectMapper()

    @Test
    fun `Should create a processed payment when the date is today`() {
        val today = LocalDate.now()

        val response = post(paymentBody(today, "10.50", "rent", "a@b.com"))

        val body = objectMapper.readTree(response.body)
        assertAll("Assert created payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(201)))
            assertThat(response.headers.location.toString(), `is`(equalTo("/api/payment/${body["pk"].asText()}")))
            assertPaymentBody(body, "PROCESSED_PAYMENT", today, "10.50", "rent", "a@b.com")
        })
    }

    @Test
    fun `Should create a scheduled payment when the date is in the future`() {
        val date = LocalDate.now().plusDays(3)

        val response = post(paymentBody(date, "99.99", "insurance", "b@c.com"))

        val body = objectMapper.readTree(response.body)
        assertAll("Assert created payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(201)))
            assertThat(response.headers.location.toString(), `is`(equalTo("/api/payment/${body["pk"].asText()}")))
            assertPaymentBody(body, "SCHEDULED_PAYMENT", date, "99.99", "insurance", "b@c.com")
        })
    }

    @Test
    fun `Should create a payment without description and return it as null`() {
        val today = LocalDate.now()

        val response = post("""{"date":"$today","value":1,"credit":{"pix_key":"x"}}""")

        val body = objectMapper.readTree(response.body)
        assertAll("Assert created payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(201)))
            assertThat(body.has("description"), `is`(equalTo(true)))
            assertThat(body["description"].isNull, `is`(equalTo(true)))
        })
    }

    @Test
    fun `Should return 400 with the error format when the payment is invalid`() {
        val response = post("""{"value":0,"credit":{"pix_key":"a@b.com"}}""")

        val body = objectMapper.readTree(response.body)
        val messages = body["errors"].map { it["message"].asText() }.toSet()
        assertAll("Assert validation error", {
            assertThat(response.statusCode.value(), `is`(equalTo(400)))
            assertThat(body.fieldNames().asSequence().toList(), `is`(equalTo(listOf("errors"))))
            assertThat(messages, `is`(equalTo(setOf("value must be greater than or equal to 0.01", "date must not be null"))))
        })
    }

    @Test
    fun `Should find the payment by id`() {
        val date = LocalDate.now().plusDays(1)
        val pk = createdPk(post(paymentBody(date, "10.50", "rent", "a@b.com")))

        val response = restTemplate.getForEntity("/api/payment/$pk", String::class.java)

        val body = objectMapper.readTree(response.body)
        assertAll("Assert found payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(200)))
            assertThat(body["pk"].asText(), `is`(equalTo(pk)))
            assertPaymentBody(body, "SCHEDULED_PAYMENT", date, "10.50", "rent", "a@b.com")
        })
    }

    @Test
    fun `Should return 404 when the payment does not exist`() {
        val id = "00000000-0000-0000-0000-000000000000"

        val response = restTemplate.getForEntity("/api/payment/$id", String::class.java)

        assertAll("Assert not found", {
            assertThat(response.statusCode.value(), `is`(equalTo(404)))
            assertThat(errorMessages(response), `is`(equalTo(listOf("Payment $id not found"))))
        })
    }

    @Test
    fun `Should delete a scheduled payment`() {
        val pk = createdPk(post(paymentBody(LocalDate.now().plusDays(2), "5.00", "gym", "c@d.com")))

        val response = delete(pk)
        val afterDelete = restTemplate.getForEntity("/api/payment/$pk", String::class.java)

        assertAll("Assert deleted payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(204)))
            assertThat(response.body, `is`(nullValue()))
            assertThat(afterDelete.statusCode.value(), `is`(equalTo(404)))
        })
    }

    @Test
    fun `Should return 422 when deleting a processed payment`() {
        val pk = createdPk(post(paymentBody(LocalDate.now(), "5.00", "coffee", "d@e.com")))

        val response = delete(pk)
        val afterDelete = restTemplate.getForEntity("/api/payment/$pk", String::class.java)

        assertAll("Assert not deleted payment", {
            assertThat(response.statusCode.value(), `is`(equalTo(422)))
            assertThat(errorMessages(response), `is`(equalTo(listOf("Payment processed $pk can't be change"))))
            assertThat(afterDelete.statusCode.value(), `is`(equalTo(200)))
        })
    }

    @Test
    fun `Should return 422 when deleting a payment that does not exist`() {
        val id = "11111111-1111-1111-1111-111111111111"

        val response = delete(id)

        assertAll("Assert deletion not allowed", {
            assertThat(response.statusCode.value(), `is`(equalTo(422)))
            assertThat(errorMessages(response), `is`(equalTo(listOf("Payment $id not found"))))
        })
    }

    private fun paymentBody(date: LocalDate, value: String, description: String, pixKey: String) =
        """{"date":"$date","value":$value,"description":"$description","credit":{"pix_key":"$pixKey"}}"""

    private fun post(body: String): ResponseEntity<String> {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        return restTemplate.postForEntity("/api/payment", HttpEntity(body, headers), String::class.java)
    }

    private fun delete(id: String): ResponseEntity<String> =
        restTemplate.exchange("/api/payment/$id", HttpMethod.DELETE, null, String::class.java)

    private fun createdPk(response: ResponseEntity<String>): String =
        objectMapper.readTree(response.body)["pk"].asText()

    private fun errorMessages(response: ResponseEntity<String>): List<String> {
        val body = objectMapper.readTree(response.body)

        return body["errors"].map { it["message"].asText() }
    }

    /** Fixes the exact set of fields and their JSON types of the current response body. */
    private fun assertPaymentBody(
        body: JsonNode,
        sk: String,
        date: LocalDate,
        value: String,
        description: String,
        pixKeyCredit: String
    ) {
        val now = Instant.now()
        assertAll("Assert payment body", {
            assertThat(
                body.fieldNames().asSequence().toList(),
                `is`(equalTo(listOf("pk", "sk", "date", "value", "description", "pixKeyCredit", "ttl")))
            )
            assertThat(body["pk"].isTextual, `is`(equalTo(true)))
            assertThat(body["sk"].asText(), `is`(equalTo(sk)))
            assertThat(body["date"].asText(), `is`(equalTo(date.toString())))
            assertThat(body["value"].isNumber, `is`(equalTo(true)))
            // POST returns 10.50 and GET returns 10.5 (BigDecimal after the DynamoDB round trip)
            assertThat(body["value"].decimalValue().compareTo(BigDecimal(value)), `is`(equalTo(0)))
            assertThat(body["description"].asText(), `is`(equalTo(description)))
            assertThat(body["pixKeyCredit"].asText(), `is`(equalTo(pixKeyCredit)))
            assertThat(body["ttl"].isIntegralNumber, `is`(equalTo(true)))
            assertThat(body["ttl"].asLong(), greaterThanOrEqualTo(now.plus(Duration.ofMinutes(59)).epochSecond))
            assertThat(body["ttl"].asLong(), lessThanOrEqualTo(now.plus(Duration.ofMinutes(61)).epochSecond))
        })
    }
}
