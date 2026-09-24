# CLAUDE.md — payment-service (module)

REST API that creates, reads and deletes payments and publishes payment events to SNS. Layered architecture. Repository-wide rules (event flow, workflows, shared conventions) are in the root `CLAUDE.md`.

Stack: Kotlin 1.8.22, **build with JDK 17** (the compiler fails on JDK 25), Spring Boot 3.1.1, Spring Cloud AWS 3.0.0, Jackson 2.

---

## Structure

```
src/main/kotlin/br/com/developers/
├── PaymentServiceApplication.kt
├── payment/
│   ├── Payment.kt                        # DynamoDB entity (pk UUID + sk event type)
│   ├── PaymentController.kt              # POST / GET / DELETE /api/payment
│   ├── PaymentService.kt                 # interface
│   ├── PaymentServiceImpl.kt             # business logic (internal class)
│   ├── PaymentRepository.kt              # @Repository over DynamoDbTemplate
│   ├── PaymentRequest.kt                 # request DTO + CreditRequest, toPayment()
│   ├── EventType.kt                      # PROCESSED_PAYMENT / SCHEDULED_PAYMENT / DELETED_PAYMENT
│   ├── TtlUtils.kt                       # ttlOf60Minutes()
│   ├── PaymentNotFoundException.kt
│   ├── PaymentDeletionNotAllowedException.kt
│   └── handler/                          # PaymentExceptionHandler, ErrorResponse, ErrorMessageResponse
└── event/
    ├── PaymentEventPublisher.kt          # publishes to SNS
    └── PaymentEventRequest.kt            # event payload (snake_case via @JsonProperty)
```

Flow: `PaymentController` → `PaymentService` → `PaymentServiceImpl` → `PaymentRepository` (DynamoDB) + `PaymentEventPublisher` (SNS).

---

## Conventions

### Services

Declare a public interface and implement it as an `internal class` with the `Impl` suffix, using constructor injection:

```kotlin
@Service
internal class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher
) : PaymentService
```

Service methods take non-null parameters (`fun findById(id: String): Payment`).

### Repositories

`@Repository` classes over `DynamoDbTemplate`, queried with `QueryConditional.keyEqualTo()` (see `PaymentRepository`). Not-found is expressed by a nullable return, and the service throws the domain exception.

### Entity

`Payment` is a `@DynamoDbBean` data class: `pk: UUID?` (partition key), `sk: String?` (sort key, holds the event type), `date`, `value: BigDecimal`, `description`, `pixKeyCredit` (attribute `pix_key_credit`) and `ttl` (default `ttlOf60Minutes()`). Every field has a default value, as the DynamoDB enhanced client requires. Table name is derived from the class name (`payment`).

### DTOs

`PaymentRequest` uses Bean Validation (`@field:NotNull`, `@field:FutureOrPresent`, `@field:DecimalMin("0.01")`) and a nested `CreditRequest` (`credit.pix_key` via `@JsonProperty`). Conversion is a `toPayment()` method on the DTO, not a mapper class. There is no global `ObjectMapper` customization in this module; use `@JsonProperty` where snake_case is needed.

### Exceptions

Custom exceptions extend `RuntimeException` and are mapped in `PaymentExceptionHandler` (`@ControllerAdvice`, extends `ResponseEntityExceptionHandler`):

| Exception | Status |
|---|---|
| `MethodArgumentNotValidException` | `400` (one message per field error) |
| `PaymentNotFoundException` | `404` |
| `PaymentDeletionNotAllowedException` | `422` |
| any other `Exception` | `500` |

### Events

`PaymentServiceImpl.save()` publishes the saved payment's `sk` as `event_type`; `delete()` publishes `DELETED_PAYMENT`. `PaymentEventPublisher` sets an `event_type` message header (used by the SNS→SQS filter) and does not propagate publish failures.

---

## Tests

Unit tests (`./mvnw test`, currently `PaymentServiceTest`, `PaymentRequestTest`, `TtlUtilsTest`, `PaymentEventPublisherTest`):

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
}
```

Integration tests (`PaymentRepositoryIT`; require Docker; run with `./mvnw test -Dtest=PaymentRepositoryIT` — they are not part of `./mvnw test`):

- `@SpringBootTest(webEnvironment = NONE)` + `@Testcontainers` + `@ActiveProfiles("integration-test")`.
- `LocalStackContainer` (`localstack/localstack:0.14.3`) with `src/test/resources/localstack/` mapped to `/docker-entrypoint-initaws.d`; `init.sh` creates the `payment` table and the `payment-event` topic and prints `Initialized.`, which the container waits for.
- Endpoints are injected with `@DynamicPropertySource`.
- `application-integration-test.yml` sets `payment.topic.name: payment-event`.
