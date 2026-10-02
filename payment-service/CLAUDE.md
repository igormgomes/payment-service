# CLAUDE.md — payment-service (module)

REST API that creates, reads and deletes payments and publishes payment events to SNS. Hexagonal architecture (domain / application / adapters), the same layout as `payment-receipt-service`. Repository-wide rules (event flow, workflows, shared conventions) are in the root `CLAUDE.md`. The design is in `docs/arq-hex/` (`PRD.md`, `SDD.md`).

Stack: Kotlin 1.8.22, **build with JDK 17** (the compiler fails on JDK 25), Spring Boot 3.1.1, Spring Cloud AWS 3.0.0, Jackson 2.

---

## Structure

```
src/main/kotlin/br/com/developers/
├── PaymentServiceApplication.kt
├── infra/dynamodb/DynamoDbConfiguration.kt   # table name resolver: PaymentEntity -> `payment`
└── payment/
    ├── domain/                               # pure Kotlin
    │   ├── Payment.kt                        # data class + Payment.create() factory (pk, sk by date)
    │   ├── PaymentEvent.kt                   # event the service publishes (no Jackson)
    │   ├── EventType.kt                      # PROCESSED_PAYMENT / SCHEDULED_PAYMENT / DELETED_PAYMENT
    │   ├── TtlUtils.kt                       # ttlOf60Minutes()
    │   ├── PaymentNotFoundException.kt
    │   └── PaymentDeletionNotAllowedException.kt
    ├── application/
    │   ├── PaymentService.kt                 # implements the 3 use cases (internal class)
    │   └── port/
    │       ├── input/                        # SavePaymentUseCase, FindPaymentUseCase, DeletePaymentUseCase
    │       └── output/                       # PaymentRepositoryPort, PaymentEventPublisherPort
    └── adapters/
        ├── input/rest/                       # PaymentController, PaymentRequest (+CreditRequest), PaymentResponse
        │   └── handler/                      # PaymentExceptionHandler, ErrorResponse, ErrorMessageResponse
        └── output/
            ├── dynamodb/                     # PaymentDynamoDbAdapter, PaymentEntity, PaymentMapper
            └── sns/                          # PaymentEventSnsAdapter, PaymentEventRequest
```

Flow: `PaymentController` → use case (`PaymentService`) → `PaymentRepositoryPort` (`PaymentDynamoDbAdapter`) + `PaymentEventPublisherPort` (`PaymentEventSnsAdapter`).

### Architecture rules

- Dependencies point inward: `adapters → application → domain`.
- `domain` and `application` must not import Spring web, AWS SDK, Spring Cloud AWS, Bean Validation or Jackson. `@Service` and the SLF4J `Logger` are allowed. Check with:

  ```bash
  grep -rn -E "org\.springframework\.web|software\.amazon|io\.awspring|jakarta\.validation|com\.fasterxml\.jackson" \
    src/main/kotlin/br/com/developers/payment/domain src/main/kotlin/br/com/developers/payment/application
  ```
  (no output = OK)
- The controller only calls use cases; adapters implement the output ports. Framework classes (`@DynamoDbBean`, `@JsonProperty`, `SnsTemplate`, `DynamoDbTemplate`) stay in `adapters/` and `infra/`.

---

## Conventions

### Services

`PaymentService` (no `Impl` suffix) is an `internal class` annotated `@Service` that implements the three input ports and depends only on the two output ports, using constructor injection. Methods take non-null parameters (`fun findById(id: String): Payment`).

### Adapters

Adapters are `@Component` classes that implement an output port. `PaymentDynamoDbAdapter` runs on `DynamoDbTemplate`, queried with `QueryConditional.keyEqualTo()`; not-found is a nullable return and the service throws the domain exception. `PaymentEventSnsAdapter` reads `payment.topic.name` with `@Value`.

### Entity and mapper

`PaymentEntity` is the `@DynamoDbBean` data class: `pk: UUID?` (partition key), `sk: String?` (sort key, holds the event type), `date`, `value: BigDecimal`, `description`, `pixKeyCredit` (attribute `pix_key_credit`) and `ttl` (default `ttlOf60Minutes()`). Every field has a default value, as the DynamoDB enhanced client requires. `PaymentMapper.kt` holds the `Payment.toEntity()` / `PaymentEntity.toDomain()` extension functions.

The table name comes from `DynamoDbConfiguration` (`PaymentEntity` → `payment`). Without it the class would resolve to `payment_entity`. In Spring Cloud AWS 3.0.0 the override is `fun <T> resolve(clazz: Class<T>): String` (`<T : Any>` is from version 4).

### Domain

`Payment` is a plain data class. The rule "PROCESSED_PAYMENT if the date is today, otherwise SCHEDULED_PAYMENT" and the `pk` generation live in `Payment.create()`, not in the request DTO.

### DTOs

`PaymentRequest` uses Bean Validation (`@field:NotNull`, `@field:FutureOrPresent`, `@field:DecimalMin("0.01")`) and a nested `CreditRequest` (`credit.pix_key` via `@JsonProperty`); `toPayment()` only delegates to `Payment.create()`. `PaymentResponse` is the response body, built with `Payment.toResponse()`. There is no global `ObjectMapper` customization in this module; use `@JsonProperty` where snake_case is needed.

The response body of `POST`/`GET /api/payment` is camelCase, in this order: `pk`, `sk`, `date`, `value`, `description`, `pixKeyCredit`, `ttl` (`description` is `null`, not omitted, when absent). This is the pre-refactoring contract; `PaymentControllerIT` fixes it.

### Exceptions

Domain exceptions extend `Exception` and are mapped in `PaymentExceptionHandler` (`@ControllerAdvice`, extends `ResponseEntityExceptionHandler`):

| Exception | Status |
|---|---|
| `MethodArgumentNotValidException` | `400` (one message per field error) |
| `PaymentNotFoundException` | `404` |
| `PaymentDeletionNotAllowedException` | `422` |
| any other `Exception` | `500` |

### Events

`PaymentService.save()` publishes a `PaymentEvent` whose `eventType` is the saved payment's `sk`; `delete()` publishes `DELETED_PAYMENT`. `PaymentEventSnsAdapter` converts it to `PaymentEventRequest` (snake_case), wraps it in a Spring `Message` with an `event_type` header and calls `snsTemplate.convertAndSend(topic, message)`. It does not propagate publish failures.

Because the `Message` itself is passed as the payload, the body published on the topic is `{"payload": {"id", "event_type", "date", "pix_key_credit"}, "headers": {"event_type", "id", "timestamp"}}`, and no SNS message attribute shows up in the subscribed queue (verified on LocalStack 0.14.3). `payment-receipt-service` reads exactly this shape (`PaymentReceiptSnsRequest.payload`), so do not change it without changing the consumer. `PaymentEventIT` fixes the shape.

---

## Tests

Unit tests (`./mvnw test`): `PaymentServiceTest` (mocks of the two output ports), `PaymentTest` (factory), `PaymentRequestTest`, `PaymentMapperTest`, `TtlUtilsTest`, `PaymentEventSnsAdapterTest`. They sit in the package that mirrors the class under test.

```kotlin
@DisplayName("Payment service test")
@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {
    private val paymentRepositoryPort: PaymentRepositoryPort = mock()
    private val paymentEventPublisherPort: PaymentEventPublisherPort = mock()
    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun before() {
        paymentService = PaymentService(paymentRepositoryPort, paymentEventPublisherPort)
    }
}
```

Integration tests (require Docker; not part of `./mvnw test`; run with `./mvnw test -Dtest=<Name>IT`):

- `PaymentControllerIT` — REST contract (status, body, `Location`, error format) through `TestRestTemplate`.
- `PaymentEventIT` — payload published on the topic, read from the SQS queue `payment-event-test`.
- `PaymentDynamoDbAdapterIT` — `PaymentDynamoDbAdapter` against DynamoDB, including that items land in the `payment` table.

`PaymentControllerIT` and `PaymentEventIT` are characterization tests: they only use HTTP and the queue, so they must keep passing unchanged through refactorings.

Each IT declares its own `LocalStackContainer` (`localstack/localstack:0.14.3`, services DynamoDB/SNS/SQS) in its `companion object`, starts it in an `init` block and creates the resources it needs there with `localStack.execAwsLocal(...)` (the same pattern as `payment-receipt-service`): the `payment` table (pk + sk) in all of them, plus the `payment-event` topic in `PaymentControllerIT` and `PaymentEventIT`, and in `PaymentEventIT` also the queue `payment-event-test` subscribed to the topic. Endpoints are injected with `@DynamicPropertySource`. There is no init script; the only shared test helper is `support/LocalStackSupport.kt` (`execAwsLocal`, which retries `awslocal` until LocalStack is ready). `application-integration-test.yml` sets `payment.topic.name: payment-event`.

DynamoDB normalizes numbers (`50.00` is read back as `50`), so compare `BigDecimal` values with `compareTo`, not `equals`.

After moving or renaming classes run `./mvnw clean test`: stale `.class` files in `target/` make Spring fail with `ConflictingBeanDefinitionException`.
