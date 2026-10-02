# CLAUDE.md — payment-receipt-service (module)

Consumes payment events from SQS, stores payment receipts in DynamoDB and exposes a REST query. **Hexagonal architecture** (design docs: `docs/arq-hex/PRD.md` and `docs/arq-hex/SDD.md`). Repository-wide rules (event flow, workflows, shared conventions) are in the root `CLAUDE.md`.

Stack: Kotlin 2.3.21, **JDK 25** (Docker base image `amazoncorretto:25`), Spring Boot 4.1.1, Spring Cloud AWS 4.1.1, **Jackson 3** (`tools.jackson.*`, not `com.fasterxml.jackson.databind`; the annotations `@JsonProperty`/`@JsonInclude` stay in `com.fasterxml.jackson.annotation`).

---

## Structure

```
src/main/kotlin/br/com/developers/
├── PaymentReceiptServiceApplication.kt
├── config/MessageConverterConfiguration.kt   # JsonMapper + SQS message converter
├── infra/
│   ├── sqs/SqsConfiguration.kt               # listener factory, manual acknowledgement
│   └── dynamodb/DynamoDbConfiguration.kt     # table name resolver (payment_receipt)
└── receipt/
    ├── domain/                               # pure model: PaymentReceipt, EventType,
    │                                         # PaymentReceiptNotFoundException, TtlUtils
    ├── application/
    │   ├── PaymentReceiptService.kt          # implements both use cases
    │   └── port/
    │       ├── input/                        # SavePaymentReceiptUseCase, FindPaymentReceiptUseCase
    │       └── output/PaymentReceiptRepositoryPort.kt
    └── adapters/
        ├── input/
        │   ├── rest/                         # PaymentReceiptController + handler/
        │   └── sqs/                          # PaymentReceiptConsumer, PaymentReceiptSnsRequest
        └── output/dynamodb/                  # PaymentReceiptDynamoDbAdapter, PaymentReceiptEntity,
                                              # PaymentReceiptMapper
```

## Architecture rules

- Dependencies point inward: `adapters` → `application` → `domain`. `domain` and `application` must not import Spring web, AWS SDK, DynamoDB annotations or the persistence entity.
- `domain/PaymentReceipt` is a plain data class; the `@DynamoDbBean` lives in `PaymentReceiptEntity` (adapter). Convert with the extension functions in `PaymentReceiptMapper.kt` (`toEntity()` / `toDomain()`).
- Input adapters call **use-case interfaces** (`port/input`), never the service class directly. The service depends on `PaymentReceiptRepositoryPort`, implemented by `PaymentReceiptDynamoDbAdapter`.
- Naming differs from `payment-service`: use cases are interfaces implemented by `PaymentReceiptService` (no `Impl` suffix); adapters are `@Component` (not `@Repository`).
- The service is `internal class ... @Service`, constructor-injected.

---

## Conventions

### SQS consumer

`PaymentReceiptConsumer` (`@Component @Validated`) listens on `${payment.receipt.queue-name}` with `defaultSqsListenerContainerFactory` (manual ack, ordered). The message is an SNS envelope (`PaymentReceiptSnsRequest`, field `Message`) whose body is parsed into `PaymentReceiptSnsPayloadRequest`, converted with `toPaymentReceipt()` and passed to `SavePaymentReceiptUseCase`. The message is acknowledged only when processing succeeds; on failure it is logged and left for redelivery.

### Persistence

- Table `payment_receipt` (partition key `pk: UUID`). `DynamoDbConfiguration` maps `PaymentReceiptEntity` to that name because the default class-name resolution would produce `payment_receipt_entity`.
- Entity fields have defaults; `ttl` defaults to `ttlOf60Minutes()`; attributes are snake_case via `@DynamoDbAttribute`.
- `PaymentReceiptService.save()`: status `DELETED_PAYMENT` → `update`, otherwise `save`.

### JSON

`MessageConverterConfiguration` defines the primary `JsonMapper`: `KotlinModule`, `SNAKE_CASE`, non-null inclusion, dates not written as timestamps, `FAIL_ON_UNKNOWN_PROPERTIES` disabled. `java.time` support is built into Jackson 3 (no `JavaTimeModule`).

### Exceptions

`PaymentReceiptNotFoundException` (domain) extends `Exception`, mapped in `PaymentReceiptExceptionHandler` (`@ControllerAdvice`): not found → `404`, any other `Exception` → `500`. Error shape is in the root `CLAUDE.md`.

### Ports

`8082` only with the `local` profile; `8080` otherwise (`--server.port=$PORT` in `app.sh`).

---

## Tests

Layout mirrors the main packages. Unit tests: `PaymentReceiptServiceTest`, `PaymentReceiptConsumerTest`, `PaymentReceiptMapperTest`, `TtlUtilsTest`. Integration tests (Docker required; run with `./mvnw test -Dtest=<Name>IT`, they are not part of `./mvnw test`): `PaymentReceiptConsumerIT`, `PaymentReceiptControllerIT`, `PaymentReceiptDynamoDbAdapterIT`, `PaymentReceiptFlowIT`.

- LocalStack via `org.testcontainers.localstack.LocalStackContainer` (`localstack/localstack:3.4.0`), started in the test's `companion object`; resources are created with the helper `LocalStackSupport.execAwsLocal(...)` (in `src/test/kotlin/.../support/`) instead of an `init.sh` mapping. It runs `awslocal <args>` inside the container and retries up to 15 times, 1 s apart, failing if it never succeeds. Each IT starts its own container with only the services it needs (`sqs`, or `dynamodb` + `sqs`).
- Slice tests use `@SqsTest(PaymentReceiptConsumer::class)` with `@ImportAutoConfiguration(SqsConfiguration::class, MessageConverterConfiguration::class)`; collaborators are replaced with `@MockitoBean`.
- Endpoints/credentials are injected with `@DynamicPropertySource`; `application-integration-test.yml` sets `payment.receipt.queue-name: payment-receipt`.
- Assert async behavior with Awaitility (`await().atMost(...)`).
- Test the ports and adapters separately: use cases with mocked `PaymentReceiptRepositoryPort`, adapters against LocalStack.
