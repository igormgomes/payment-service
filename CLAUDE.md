# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Event-driven payment microservices platform built with Kotlin and Spring Boot. Two services communicate asynchronously via AWS SNS/SQS:

- **payment-service** — REST API that stores payments in DynamoDB and publishes events to SNS
- **payment-receipt-service** — SQS consumer that persists payment receipts in DynamoDB
- **infra** — AWS CDK (TypeScript) for provisioning VPC, ECS Fargate, ALB, DynamoDB, SNS, SQS

## Build & Test Commands

Both Kotlin services use Maven from their respective directories:

```bash
# Build
./mvnw clean install          # build + tests
./mvnw clean package -DskipTests  # build only

# Tests
./mvnw test                                      # all unit tests
./mvnw test -Dtest=PaymentServiceTest            # single test class
./mvnw test -Dtest=PaymentServiceTest#methodName # single test method
./mvnw verify -Dtest=PaymentReceiptConsumerIT    # integration test (requires Docker)
```

Infrastructure (from `infra/`):

```bash
npm run build   # compile TypeScript
npm run test    # Jest unit tests
cdk deploy --all
```

## Local Development

LocalStack emulates AWS services (SNS, SQS, DynamoDB, S3):

```bash
docker-compose up   # starts LocalStack and initialises all queues/topics/tables
```

The `setup-resources` container runs automatically and creates:
- SQS queues: `schedule-payment`, `payment-receipt`
- SNS topic: `payment-event`
- DynamoDB tables: `payment`, `payment_receipt`
- SNS→SQS subscription wiring both services together

Services need environment variables `PAYMENT_TOPIC_NAME` and `PAYMENT_RECEIPT_QUEUE_NAME` (ARNs/URLs from LocalStack or AWS).

## Architecture

### Message Flow

```
POST /api/payment
      │
      ▼
PaymentServiceImpl ──saves──► DynamoDB (payment table)
      │
      ▼
PaymentEventPublisher ──publishes──► SNS topic
                                          │
                                          ▼
                                    SQS queue (payment-receipt)
                                          │
                                          ▼
                               PaymentReceiptConsumer (manual ACK)
                                          │
                                          ▼
                               PaymentReceiptService ──saves──► DynamoDB (payment_receipt table)
```

### DynamoDB Key Design

**payment table** — composite key: `pk` (UUID) + `sk` (EventType name). This allows storing the full event history (SCHEDULED_PAYMENT, PROCESSED_PAYMENT, DELETED_PAYMENT) per payment UUID.

**payment_receipt table** — partition key only: `pk` (UUID).

Both tables use a 60-minute TTL attribute.

### SQS Consumer

`PaymentReceiptConsumer` uses manual acknowledgment (`SqsAcknowledgementMode.MANUAL`) configured in `SqsConfiguration`. Messages are processed in order (single listener); the listener factory is set with `maxConcurrentMessages=1`.

### Event Types

`EventType` enum: `PROCESSED_PAYMENT`, `SCHEDULED_PAYMENT`, `DELETED_PAYMENT` — used as the sort key in the payment table and as receipt status.

## Testing Patterns

- **Unit tests**: Mockito + Mockito-Kotlin; no Spring context loaded
- **Integration tests** (`*IT`): `@SpringBootTest` + `@Testcontainers` with a LocalStack container; dynamic properties registered via `@DynamicPropertySource` to point Spring Cloud AWS at the container endpoint
- Jackson `ObjectMapper` uses `SNAKE_CASE` naming strategy — match this in test payloads