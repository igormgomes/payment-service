package br.com.developers.payment

import br.com.developers.payment.support.execAwsLocal
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.LocalDate

/**
 * Characterization test of the SNS event contract: topic `payment-event` -> queue `payment-event-test` (see
 * the `companion object`). It only goes through HTTP and the queue, so it must keep passing, unchanged, through the
 * hexagonal refactoring. Requires Docker; run with `./mvnw test -Dtest=PaymentEventIT`.
 *
 * The published body is not the bare `{id, event_type, date, pix_key_credit}`: the publisher passes a Spring
 * `Message` to `SnsTemplate.convertAndSend`, which serializes the whole message, so the body is
 * `{"payload": {...}, "headers": {...}}`. `payment-receipt-service` depends on that shape
 * (`PaymentReceiptSnsRequest.payload`), so it is fixed here exactly as observed. No SNS message attribute shows up
 * in the queue envelope; `event_type` travels only inside `headers`.
 */
@DisplayName("Payment event integration test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class PaymentEventIT {

    companion object {
        private const val QUEUE_NAME = "payment-event-test"

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
            localStack.execAwsLocal("sqs", "create-queue", "--queue-name", QUEUE_NAME)
            localStack.execAwsLocal(
                "sns", "subscribe",
                "--topic-arn", "arn:aws:sns:${localStack.region}:000000000000:payment-event",
                "--protocol", "sqs",
                "--notification-endpoint", "arn:aws:sqs:${localStack.region}:000000000000:$QUEUE_NAME"
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
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var sqsAsyncClient: SqsAsyncClient

    private val objectMapper = ObjectMapper()

    @Test
    fun `Should publish a processed payment event when the date is today`() {
        val today = LocalDate.now()
        val pk = createPayment(today, "a@b.com")

        val notification = awaitNotification(pk, "PROCESSED_PAYMENT")

        assertEvent(notification, pk, "PROCESSED_PAYMENT", today, "a@b.com")
    }

    @Test
    fun `Should publish a scheduled payment event when the date is in the future`() {
        val date = LocalDate.now().plusDays(3)
        val pk = createPayment(date, "b@c.com")

        val notification = awaitNotification(pk, "SCHEDULED_PAYMENT")

        assertEvent(notification, pk, "SCHEDULED_PAYMENT", date, "b@c.com")
    }

    @Test
    fun `Should publish a deleted payment event when a scheduled payment is deleted`() {
        val date = LocalDate.now().plusDays(2)
        val pk = createPayment(date, "c@d.com")

        restTemplate.exchange("/api/payment/$pk", HttpMethod.DELETE, null, String::class.java)

        val notification = awaitNotification(pk, "DELETED_PAYMENT")
        assertEvent(notification, pk, "DELETED_PAYMENT", date, "c@d.com")
    }

    @Test
    fun `Should not publish a deleted payment event when the deletion of a processed payment is rejected`() {
        val today = LocalDate.now()
        val pk = createPayment(today, "d@e.com")

        val response = restTemplate.exchange("/api/payment/$pk", HttpMethod.DELETE, null, String::class.java)

        // the processed event proves the queue works; the rejected deletion must not add a deleted event
        val eventTypes = collectEventTypes(pk)
        assertAll("Assert only the processed event was published", {
            assertThat(response.statusCode.value(), `is`(equalTo(422)))
            assertThat(eventTypes, `is`(equalTo(listOf("PROCESSED_PAYMENT"))))
        })
    }

    private fun createPayment(date: LocalDate, pixKey: String): String {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = """{"date":"$date","value":10.50,"description":"rent","credit":{"pix_key":"$pixKey"}}"""

        val response = restTemplate.postForEntity("/api/payment", HttpEntity(body, headers), String::class.java)

        return objectMapper.readTree(response.body)["pk"].asText()
    }

    /**
     * Reads the queue until the notification for this payment and event type shows up. Messages of other payments
     * (the queue is shared with the other integration tests) are deleted and ignored.
     */
    private fun awaitNotification(pk: String, eventType: String): JsonNode {
        val queueUrl = sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build())
            .get().queueUrl()

        repeat(10) {
            val messages = sqsAsyncClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .build()
            ).get().messages()

            messages.forEach { message ->
                sqsAsyncClient.deleteMessage(
                    DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build()
                ).get()
            }
            messages.map { objectMapper.readTree(it.body()) }
                .firstOrNull {
                    val payload = objectMapper.readTree(it["Message"].asText())["payload"]
                    payload["id"].asText() == pk && payload["event_type"].asText() == eventType
                }
                ?.let { return it }
        }
        throw AssertionError("No $eventType event published for payment $pk")
    }

    /**
     * Reads the queue for a fixed window and returns the event types published for this payment. Messages of other
     * payments (the queue is shared with the other integration tests) are deleted and ignored.
     */
    private fun collectEventTypes(pk: String): List<String> {
        val queueUrl = sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build())
            .get().queueUrl()
        val eventTypes = mutableListOf<String>()

        repeat(3) {
            val messages = sqsAsyncClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .build()
            ).get().messages()

            messages.forEach { message ->
                sqsAsyncClient.deleteMessage(
                    DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build()
                ).get()
                val payload = objectMapper.readTree(objectMapper.readTree(message.body())["Message"].asText())["payload"]
                if (payload["id"].asText() == pk) {
                    eventTypes.add(payload["event_type"].asText())
                }
            }
        }
        return eventTypes
    }

    private fun assertEvent(notification: JsonNode, pk: String, eventType: String, date: LocalDate, pixKey: String) {
        val message = objectMapper.readTree(notification["Message"].asText())
        val payload = message["payload"]
        val headers = message["headers"]

        assertAll("Assert published event", {
            assertThat(message.fieldNames().asSequence().toList(), `is`(equalTo(listOf("payload", "headers"))))
            assertThat(payload.fieldNames().asSequence().toList(), `is`(equalTo(listOf("id", "event_type", "date", "pix_key_credit"))))
            assertThat(payload["id"].asText(), `is`(equalTo(pk)))
            assertThat(payload["event_type"].asText(), `is`(equalTo(eventType)))
            assertThat(payload["date"].asText(), `is`(equalTo(date.toString())))
            assertThat(payload["pix_key_credit"].asText(), `is`(equalTo(pixKey)))
            assertThat(headers.fieldNames().asSequence().toSet(), `is`(equalTo(setOf("event_type", "id", "timestamp"))))
            assertThat(headers["event_type"].asText(), `is`(equalTo(eventType)))
            assertThat(headers["id"].isTextual, `is`(equalTo(true)))
            assertThat(headers["timestamp"].isNumber, `is`(equalTo(true)))
        })
    }
}
