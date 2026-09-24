# CLAUDE.md — payment-service

This file documents the codebase for AI assistants working in this repository.
Module-specific conventions live next to each module:

- `payment-service/CLAUDE.md` — layered REST API
- `payment-receipt-service/CLAUDE.md` — hexagonal SQS consumer

Claude Code loads a module's file when working inside that directory.

## Project Overview

Event-driven payment processing platform built as two independent microservices backed by AWS managed services. Payments are created via a REST API, persisted in DynamoDB, and trigger SNS events that a second service consumes to build payment receipts.

See `assets/draw.jpg` for the architecture diagram.

---

## Repository Structure

```
payment-service/            # REST API microservice (layered)
payment-receipt-service/    # SQS consumer + REST query microservice (hexagonal)
  docs/arq-hex/             # PRD.md and SDD.md for the hexagonal design
infra/                      # AWS CDK infrastructure (TypeScript)
docker-compose.yml          # LocalStack local development environment
assets/                     # Architecture diagrams
.claude/agents/             # Project subagents (see "Claude Code agents")
```

The two services are independent Maven projects (each with its own `./mvnw` and `pom.xml`). They deliberately differ in architecture and in framework/JDK versions, so never assume a convention from one applies to the other.

---

## Modules at a glance

| | `payment-service` | `payment-receipt-service` |
|---|---|---|
| Role | REST API, publishes events to SNS | Consumes SQS, stores receipts, REST query |
| Architecture | Layered (controller → service → repository) | Hexagonal (domain / application / adapters) |
| Kotlin | 1.8.22 | 2.3.21 |
| JDK to build with | 17 | 25 |
| Spring Boot | 3.1.1 | 4.1.1 |
| Spring Cloud AWS | 3.0.0 | 4.1.1 |
| JSON | Jackson 2 (`com.fasterxml.jackson`) | Jackson 3 (`tools.jackson`; `@JsonProperty`/`@JsonInclude` annotations remain in `com.fasterxml.jackson.annotation`) |
| Docker base image | `amazoncorretto:17` | `amazoncorretto:25` |
| LocalStack in ITs | `localstack/localstack:0.14.3` | `localstack/localstack:3.4.0` |
| Server port | 8080 | 8080 (AWS); 8082 with the `local` profile |

Always confirm versions in the module's `pom.xml` before relying on this table.

Shared stack: Spring Cloud AWS (SNS publish, SQS consume, DynamoDB), AWS ECS Fargate (1–10 tasks, auto-scaling), AWS CDK (TypeScript) in `infra/`, LocalStack for local AWS, JUnit 5 + Mockito-Kotlin + Hamcrest + TestContainers for tests.

---

## API Reference

### payment-service (`:8080`)

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/payment` | `201 Created` (+ `Location`) | `400` Bean Validation failure |
| `GET` | `/api/payment/{id}` | `200 OK` | `404` not found |
| `DELETE` | `/api/payment/{id}` | `204 No Content` | `422` payment is `PROCESSED_PAYMENT`, or does not exist (current behavior) |
| `GET` | `/actuator/health` | `200 OK` | — |

Request body of `POST /api/payment`:

```json
{ "date": "2026-10-01", "value": 10.50, "description": "rent", "credit": { "pix_key": "a@b.com" } }
```

`sk` is `PROCESSED_PAYMENT` when `date` is today, otherwise `SCHEDULED_PAYMENT`.

### payment-receipt-service (`:8080` in AWS, `:8082` with `local` profile)

| Method | Path | Success | Errors |
|---|---|---|---|
| `GET` | `/api/payment-receipt/{id}` | `200 OK` | `404` not found |
| `GET` | `/actuator/health` | `200 OK` | — |

Both services return unhandled exceptions as `500`. Error response shape:

```json
{ "errors": [{ "message": "..." }] }
```

---

## Event Flow

```
POST /api/payment → PaymentServiceImpl.save()
  → DynamoDB table `payment` (pk = UUID, sk = event type)
  → PaymentEventPublisher → SNS topic `payment-event` (message attribute: event_type)
      → SQS `payment-receipt`    (all events)
      → SQS `schedule-payment`   (CDK filter: event_type = SCHEDULED_PAYMENT;
                                  docker-compose subscribes without a filter)
      → e-mail subscription      (address set in infra/lib/sns-stack.ts)

DELETE /api/payment/{id} → publishes DELETED_PAYMENT the same way

PaymentReceiptConsumer (@SqsListener on `payment-receipt`)
  → unwrap SNS envelope → SavePaymentReceiptUseCase → PaymentReceiptService
  → PaymentReceiptRepositoryPort → DynamoDB table `payment_receipt`
     (status DELETED_PAYMENT updates the receipt, any other status saves it)
```

Event types: `PROCESSED_PAYMENT`, `SCHEDULED_PAYMENT`, `DELETED_PAYMENT`.

Behavior to know: `PaymentEventPublisher` catches and logs publish failures instead of propagating them, so a payment can be saved without its event being published. The SQS consumer acknowledges a message only after successful processing.

---

## Development Workflows

### JDK

Each module needs its own JDK (see the table above). Kotlin 1.8.22 cannot parse newer JDK versions and fails to compile on JDK 25 (`IllegalArgumentException: 25.0.4.1`), so build `payment-service` with JDK 17. On macOS: `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw ...`.

### Local development (LocalStack)

```bash
docker-compose up
```

Starts LocalStack (`http://localhost:4566`, image `localstack/localstack:latest`, not pinned; the integration tests pin their own versions, see the table above) and a setup container that creates the SNS topic, both SQS queues, the subscriptions and the DynamoDB tables `payment` (pk + sk) and `payment_receipt` (pk).

Run each service with the `local` profile (the version in the jar name comes from the pom):

```bash
cd payment-service
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean package -DskipTests
java -jar target/payment-service-3.0.3.jar --spring.profiles.active=local

cd payment-receipt-service
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./mvnw clean package -DskipTests
java -jar target/payment-receipt-service-3.0.3.jar --spring.profiles.active=local
```

### Running tests

Run from the module directory.

```bash
./mvnw test                          # unit tests (*Test.kt) only
./mvnw test -Dtest=SomeClassIT       # a specific integration test (requires Docker)
```

Surefire's default includes do not match `*IT` and no failsafe plugin is configured, so `./mvnw test` never runs the integration tests; select them with `-Dtest=`.

**Never run `./mvnw verify`, `install` or `deploy`.** Both poms bind `io.fabric8:docker-maven-plugin` to `pre-integration-test` (build + start the image) and `post-integration-test` (**push to `registry.hub.docker.com/igormgomes`**). `package` and `test` are safe.

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
docker build . -t payment-service:latest           # from payment-service/
docker build . -t payment-receipt-service:latest   # from payment-receipt-service/
```

---

## Configuration Profiles

| Profile | Description |
|---|---|
| *(default)* | AWS; topic/queue names come from env vars |
| `local` | LocalStack at `http://localhost:4566` |
| `integration-test` | TestContainers LocalStack (dynamic endpoints via `@DynamicPropertySource`) |

| Variable | Used by | Description |
|---|---|---|
| `PAYMENT_TOPIC_NAME` | payment-service | SNS topic name |
| `PAYMENT_RECEIPT_QUEUE_NAME` | payment-receipt-service | SQS queue name |
| `PORT` | both (`app.sh`) | Passed as `--server.port` |

In the `local` profile `payment-service` uses the topic ARN `arn:aws:sns:us-east-1:000000000000:payment-event` and `payment-receipt-service` uses queue `payment-receipt`. AWS region defaults to `us-east-1` in both `application.yml` files.

---

## Conventions shared by both services

### Deprecated APIs

Never call a class, method, or dependency coordinate marked `@Deprecated` (or documented as deprecated) — check for a current replacement first, especially right after a major-version dependency bump. If a deprecated API is genuinely the only option, call it out explicitly in the response instead of leaving it unmentioned in the diff.

### Code

- Base package `br.com.developers`.
- Constructor injection only, never `@Autowired` on fields.
- DynamoDB entities are `@DynamoDbBean` data classes with a default value on every field and a `ttl` (60 minutes, via `ttlOf60Minutes()`).
- Monetary values are `BigDecimal`.
- Use `checkNotNull()` for nullable values that must be non-null at runtime.
- Custom exceptions are mapped in a `@ControllerAdvice` under a `handler/` package, returning the error shape above.
- Logging: `LoggerFactory.getLogger(javaClass)`, INFO for business events, string templates.

### Tests

- Class `<Subject>Test.kt` (unit) or `<Subject>IT.kt` (integration); methods use backtick names: `` `Should do X when Y` ``.
- Unit tests: `@ExtendWith(MockitoExtension::class)`, dependencies via `mock()` (mockito-kotlin), subject built in `@BeforeEach`, Hamcrest `assertThat(result, is(equalTo(expected)))`, `assertAll {}`, `assertThrows<T> {}`, `argumentCaptor<T>()`.
- Integration tests: `@Testcontainers`, `@ActiveProfiles("integration-test")`, LocalStack container (`payment-service` maps an `init.sh`; `payment-receipt-service` creates resources with `LocalStackSupport.execAwsLocal(...)`, see its `CLAUDE.md`). Async behavior is asserted with Awaitility (`await().atMost(Duration.ofSeconds(n)).untilAsserted { ... }`) in `payment-receipt-service` only; Awaitility is not a dependency of `payment-service`.

Module-specific details are in each module's `CLAUDE.md`.

---

## Git workflow

- Changes go through short-lived branches and pull requests.
- Commit subjects use a lowercase type prefix, imperative and short: `feature:`, `refactor:`, `test:`, `fix:`, `docs:`.
- Do not commit personal Claude files: `.claude/settings.local.json` and `CLAUDE.local.md` are git-ignored.

---

## Claude Code agents

Project subagents live in `.claude/agents/`:

| Agent | Purpose |
|---|---|
| `code-reviewer` | Reviews changes per module style, payments/messaging risks and tests (read-only) |
| `infra-reviewer` | Reviews `infra/` CDK and LocalStack consistency (read-only) |
| `test-writer` | Writes unit/integration tests in the repo's style |
| `docs-sync` | Keeps `CLAUDE.md` and `README.md` in sync with the code |
| `commit-organizer` | Groups pending changes into commits with the repo's message style |
| `developer` | Fixes production code from a concrete list of findings (no tests, no commits) |
| `feature-flow` | Orchestrator: `test-writer` → `code-reviewer` → `developer` fixes → repeat (max 3 rounds) until no Crítico/Importante findings |

Run the pipeline on uncommitted changes with `@agent-feature-flow`. It never commits; use `commit-organizer` afterwards.
