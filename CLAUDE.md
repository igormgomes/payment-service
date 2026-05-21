# CLAUDE.md — payment-service

This file documents the codebase for AI assistants working in this repository.

## Project Overview

Event-driven payment processing platform built as two independent microservices backed by AWS managed services. Payments are created via a REST API, persisted in DynamoDB, and trigger SNS events that a second service consumes to build payment receipts.

See `assets/draw.jpg` for the architecture diagram.

---

## Repository Structure

```
payment-service/            # REST API microservice (ports 8080)
payment-receipt-service/    # SQS consumer microservice (port 8082)
infra/                      # AWS CDK infrastructure (TypeScript)
docker-compose.yml          # LocalStack local development environment
assets/                     # Architecture diagrams
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.8.22 + Java 17 |
| Framework | Spring Boot 3.1.1 + Spring Cloud AWS 3.0.0 |
| Messaging | AWS SNS (publish) + SQS (consume) |
| Database | AWS DynamoDB |
| Compute | AWS ECS Fargate (1–10 tasks, auto-scaling) |
| Infrastructure | AWS CDK 2.13.0 (TypeScript) |
| Local AWS | LocalStack (via Docker) |
| Build | Maven (per-service `./mvnw`) / NPM (infra) |
| Testing | JUnit 5, Mockito-Kotlin, TestContainers + LocalStack |

---

## Module Structure

### payment-service

```
src/main/kotlin/br/com/developers/
├── PaymentServiceApplication.kt
├── payment/
│   ├── Payment.kt                        # DynamoDB entity (pk + sk)
│   ├── PaymentController.kt              # REST: POST/GET/DELETE /api/payment
│   ├── PaymentService.kt                 # Interface
│   ├── PaymentServiceImpl.kt             # Business logic implementation
│   ├── PaymentRepository.kt              # DynamoDB data access
│   ├── PaymentRequest.kt                 # Request DTO (validated)
│   ├── PaymentNotFoundException.kt
│   ├── PaymentDeletionNotAllowedException.kt
│   ├── EventType.java                    # Enum: PROCESSED/SCHEDULED/DELETED_PAYMENT
│   └── handler/
│       ├── PaymentExceptionHandler.kt    # @ControllerAdvice
│       ├── ErrorResponse.kt
│       └── ErrorMessageResponse.kt
└── event/
    ├── PaymentEventPublisher.kt           # Publishes to SNS on save/delete
    └── PaymentEventRequest.kt
```

**Layer flow:** `PaymentController` → `PaymentService` (interface) → `PaymentServiceImpl` → `PaymentRepository` → DynamoDB + `PaymentEventPublisher` → SNS

### payment-receipt-service

```
src/main/kotlin/br/com/developers/
├── PaymentReceiptServiceApplication.kt
├── config/
│   └── MessageConverterConfiguration.kt  # Jackson ObjectMapper for SQS messages
├── infra/sqs/
│   └── SqsConfiguration.kt               # SqsMessageListenerContainerFactory (manual ack)
└── receipt/
    ├── PaymentReceipt.kt                  # DynamoDB entity (pk only)
    ├── PaymentReceiptController.kt        # REST: GET /api/payment-receipt/{id}
    ├── PaymentReceiptService.kt           # Interface
    ├── PaymentReceiptServiceImpl.kt
    ├── PaymentReceiptRepository.kt
    ├── PaymentReceiptConsumer.kt          # @SqsListener on payment-receipt queue
    ├── PaymentReceiptSnsRequest.kt        # SNS envelope wrapper
    ├── EventType.kt
    ├── PaymentReceiptNotFoundException.kt
    └── handler/
        ├── PaymentReceiptExceptionHandler.kt
        ├── ErrorResponse.kt
        └── ErrorMessageResponse.kt
```

### infra (AWS CDK)

```
bin/infra.ts                # Stack entry point and dependency wiring
lib/
├── vpc-stack.ts
├── cluster-stack.ts
├── payment-dynamodb-stack.ts
├── payment-receipt-dynamodb-stack.ts
├── sns-stack.ts            # SNS topic + SQS queues + subscriptions
├── payment-service-stack.ts
└── payment-receipt-service-stack.ts
```

---

## API Reference

### payment-service (`:8080`)

| Method | Path | Success | Error |
|---|---|---|---|
| `POST` | `/api/payment` | `201 Created` | `422` (validation) |
| `GET` | `/api/payment/{id}` | `200 OK` | `404` not found |
| `DELETE` | `/api/payment/{id}` | `204 No Content` | `422` (processed payments cannot be deleted) |
| `GET` | `/actuator/health` | `200 OK` | — |

### payment-receipt-service (`:8082`)

| Method | Path | Success | Error |
|---|---|---|---|
| `GET` | `/api/payment-receipt/{id}` | `200 OK` | `404` not found |
| `GET` | `/actuator/health` | `200 OK` | — |

**Error response shape:**
```json
{
  "errors": [{ "message": "..." }]
}
```

---

## Event Flow

```
PaymentServiceImpl.save()
  → PaymentEventPublisher → SNS topic: payment-event
    → SQS: payment-receipt (all events)
    → SQS: schedule-payment (filter: event_type = SCHEDULED_PAYMENT)
    → Email: igormgomes94@gmail.com

PaymentReceiptConsumer (@SqsListener: payment-receipt)
  → unwrap SNS envelope (PaymentReceiptSnsRequest)
  → PaymentReceiptServiceImpl.save()
  → DynamoDB (payment-receipt table)
```

---

## Development Workflows

### Local development (LocalStack)

```bash
docker-compose up
```

This starts LocalStack and an init container that creates the SNS topic, SQS queues, and DynamoDB tables. Services connect to `http://localhost:4566`.

Run each service with the `local` profile:

```bash
cd payment-service
./mvnw clean package -DskipTests
java -jar target/payment-service-3.0.3.jar --spring.profiles.active=local

cd payment-receipt-service
./mvnw clean package -DskipTests
java -jar target/payment-receipt-service-3.0.3.jar --spring.profiles.active=local
```

### Running tests

```bash
# Unit tests only
./mvnw test

# Unit + integration tests (requires Docker for TestContainers)
./mvnw verify
```

### Infrastructure

```bash
cd infra
npm install
npm run build

cdk deploy --all    # deploy all stacks to AWS
cdk destroy --all   # tear down all stacks
```

### Docker image build

```bash
docker build . -t payment-service:latest
docker build . -t payment-receipt-service:latest
```

---

## Configuration Profiles

| Profile | Description |
|---|---|
| *(default)* | AWS production; uses env vars for topic/queue names |
| `local` | LocalStack at `http://localhost:4566` |
| `integration-test` | TestContainers LocalStack (dynamic port via `@DynamicPropertySource`) |

### Key environment variables

| Variable | Used by | Description |
|---|---|---|
| `PAYMENT_TOPIC_NAME` | payment-service | SNS topic name |
| `PAYMENT_RECEIPT_QUEUE_NAME` | payment-receipt-service | SQS queue name |

AWS region defaults to `us-east-1` in both `application.yml` files.

---

## Code Conventions

### Package structure

Base package: `br.com.developers`. Domain classes live directly under the feature package (e.g., `br.com.developers.payment`). Exception handlers live in a `handler/` subpackage.

### Service pattern

Always declare a public interface and implement it as an `internal class` with the `Impl` suffix:

```kotlin
interface PaymentService {
    fun save(payment: Payment?)
    fun findById(id: String?): Payment
}

@Service
internal class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher
) : PaymentService { ... }
```

Use constructor injection everywhere — never `@Autowired` on fields.

### Repositories

Use `@Component` (not `@Repository`) with `DynamoDbTemplate` injected via constructor:

```kotlin
@Component
class PaymentRepository(private val dynamoDbTemplate: DynamoDbTemplate) {
    fun findByPk(id: String): Payment? { ... }
}
```

Query with `QueryConditional.keyEqualTo()` and `DynamoDbOperations.query()`.

### DynamoDB entities

```kotlin
@DynamoDbBean
data class Payment(
    @get:DynamoDbPartitionKey var pk: String? = null,
    var sk: String? = null,
    var ttl: Long? = null,  // always include TTL (60-minute expiration)
    ...
)
```

- All entity fields must have default values (required by DynamoDB enhanced client)
- TTL is always included (60-minute window)

### DTOs and mapping

Use data classes with `@JsonProperty` for snake_case JSON keys:

```kotlin
data class PaymentRequest(
    @JsonProperty("pix_key_credit") @field:NotBlank val pixKeyCredit: String? = null,
    @JsonProperty("value") @field:DecimalMin("0.01") val value: BigDecimal? = null,
)
```

Provide factory/conversion functions (e.g., `toPayment()`) rather than mapper classes.

### Null safety

Use `checkNotNull()` for nullable parameters that must be non-null at runtime:

```kotlin
fun findById(id: String?): Payment {
    checkNotNull(id) { "id must not be null" }
    ...
}
```

### Exception handling

Define custom exceptions as simple classes extending `RuntimeException`. Register handlers in a `@ControllerAdvice` class inside the `handler/` subpackage:

```kotlin
@ControllerAdvice
class PaymentExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(PaymentNotFoundException::class)
    fun handlePaymentNotFoundException(ex: PaymentNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity(ErrorResponse(...), HttpStatus.NOT_FOUND)
}
```

### Logging

```kotlin
private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)
log.info("Payment saved ${payment.pk}")
```

Log at INFO level for business events. Use string templates.

### JSON serialization

`MessageConverterConfiguration` configures a global `ObjectMapper` with:
- `JavaTimeModule` (LocalDate support)
- `KotlinModule`
- `SNAKE_CASE` property naming
- Non-null serialization only

---

## Testing Conventions

### Unit tests (`*Test.kt`)

```kotlin
@DisplayName("Payment service test")
@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {
    private val paymentRepository: PaymentRepository = mock()
    private val paymentEventPublisher: PaymentEventPublisher = mock()
    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun before() {
        paymentService = PaymentServiceImpl(paymentRepository, paymentEventPublisher)
    }

    @Test
    fun `Should save a valid payment`() { ... }
}
```

- Mock all dependencies via `mock()` (mockito-kotlin DSL)
- Instantiate the subject via constructor in `@BeforeEach`
- Use Hamcrest: `assertThat(result, is(equalTo(expected)))`
- Use `assertAll {}` for grouping related assertions
- Use `assertThrows<ExceptionType> { }` for exception paths
- Use `argumentCaptor<T>()` to verify objects passed to mocks

### Integration tests (`*IT.kt`)

```kotlin
@Testcontainers
@ActiveProfiles("integration-test")
@SqsTest(PaymentReceiptConsumer::class)
@ImportAutoConfiguration(SqsConfiguration::class, MessageConverterConfiguration::class)
class PaymentReceiptConsumerIT {
    @Container
    private val localStack: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack"))
        .withClasspathResourceMapping("/localstack/", "/docker-entrypoint-initaws.d", BindMode.READ_ONLY)
        .withServices(SQS)

    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
        registry.add("spring.cloud.aws.sqs.endpoint") { localStack.getEndpointOverride(SQS).toString() }
    }
}
```

- Use `@SqsTest` for focused SQS slice (avoids full Spring context)
- Init SQS resources in `src/test/resources/localstack/init.sh`
- Assert async behavior with `await().atMost(5, SECONDS).untilAsserted { ... }`

### Test naming

- Class: `<Subject>Test.kt` or `<Subject>IT.kt`
- Method: Kotlin backtick names — `` `Should do X when Y` ``
