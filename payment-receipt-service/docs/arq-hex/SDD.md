# SDD: Refatoração do payment-receipt-service para Arquitetura Hexagonal

## 1. Contexto e Escopo
O payment-receipt-service é um microsserviço Kotlin/Spring Boot que consome eventos SNS via fila SQS
(payment-receipt), persiste recibos de pagamento em DynamoDB e expõe um endpoint REST de consulta. Este
design cobre a reorganização interna do código em camadas hexagonais (domain/application/adapters),
sem qualquer alteração de contrato externo, infraestrutura ou stack tecnológica.

## 2. Visão Geral da Arquitetura

```
                    ┌─────────────────────────────────────────────┐
                    │                  DOMAIN                       │
                    │  PaymentReceipt (modelo puro, sem @DynamoDbBean) │
                    │  EventType                                    │
                    │  PaymentReceiptNotFoundException              │
                    └───────────────────▲───────────────────────────┘
                                         │ usa
                    ┌────────────────────┴───────────────────────────┐
                    │                APPLICATION                      │
                    │  port/input:  SavePaymentReceiptUseCase          │
                    │               FindPaymentReceiptUseCase          │
                    │  port/output: PaymentReceiptRepositoryPort        │
                    │  PaymentReceiptService (implementa os use cases)  │
                    └───▲───────────────────────────────────────▲──────┘
                        │ implementa                             │ implementa
      ┌─────────────────┴──────────────┐         ┌───────────────┴────────────────┐
      │      ADAPTERS - INPUT           │         │       ADAPTERS - OUTPUT         │
      │  rest/PaymentReceiptController  │         │  dynamodb/PaymentReceiptAdapter  │
      │  sqs/PaymentReceiptConsumer     │         │  dynamodb/PaymentReceiptEntity   │
      └──────────────────────────────────┘         │  dynamodb/PaymentReceiptMapper   │
                                                     └───────────────────────────────┘
```

Fluxo de dependência: adapters → application (ports) → domain. O domínio não conhece Spring, AWS SDK
ou anotações de framework. Os adapters de entrada dependem apenas das interfaces de caso de uso; o
serviço de aplicação depende apenas da interface de porta de saída, nunca da implementação DynamoDB.

## 3. Componentes e Responsabilidades
| Componente | Responsabilidade | Tecnologia |
|------------|-----------------|------------|
| `domain.PaymentReceipt` | Modelo de domínio puro (sem anotações de persistência) | Kotlin puro |
| `domain.EventType` | Enum de tipos de evento | Kotlin puro |
| `domain.PaymentReceiptNotFoundException` | Exceção de domínio | Kotlin puro |
| `application.port.input.SavePaymentReceiptUseCase` | Porta de entrada: salvar recibo | Interface Kotlin |
| `application.port.input.FindPaymentReceiptUseCase` | Porta de entrada: buscar recibo por id | Interface Kotlin |
| `application.port.output.PaymentReceiptRepositoryPort` | Porta de saída: persistência | Interface Kotlin |
| `application.PaymentReceiptService` | Orquestra casos de uso, sem lógica de infraestrutura | Kotlin (`@Service`) |
| `adapters.input.rest.PaymentReceiptController` | Expõe `GET /api/payment-receipt/{id}` | Spring Web |
| `adapters.input.sqs.PaymentReceiptConsumer` | Consome fila SQS, desencapsula envelope SNS | Spring Cloud AWS SQS |
| `adapters.output.dynamodb.PaymentReceiptDynamoDbAdapter` | Implementa `PaymentReceiptRepositoryPort` | Spring Cloud AWS DynamoDB |
| `adapters.output.dynamodb.PaymentReceiptEntity` | Entidade `@DynamoDbBean`, separada do domínio | AWS SDK Enhanced Client |
| `adapters.output.dynamodb.PaymentReceiptMapper` | Converte entidade ↔ domínio | Kotlin puro |

## 4. Modelo de Dados
Sem mudança de esquema. O modelo de domínio `PaymentReceipt` (application/domain) deixa de carregar
anotações `@DynamoDbBean`/`@DynamoDbPartitionKey`; essas anotações migram para
`adapters.output.dynamodb.PaymentReceiptEntity`, que espelha os mesmos campos (pk + demais atributos,
incluindo TTL de 60 minutos) e é convertida de/para o domínio pelo `PaymentReceiptMapper`.

## 5. Design de API / Interface
Nenhuma mudança de contrato:
- `GET /api/payment-receipt/{id}` → `200 OK` (corpo igual ao atual) | `404` (`PaymentReceiptNotFoundException`)
- `GET /actuator/health` → `200 OK`
- Consumo SQS: mesmo formato de envelope SNS (`PaymentReceiptSnsRequest` → `PaymentReceiptSnsPayloadRequest`),
  mesma fila (`payment.receipt.queue-name`), mesmo ack manual pós-sucesso.

## 6. Fluxos de Sequência

**Fluxo principal (consumo SQS → persistência):**
1. `PaymentReceiptConsumer` (adapter input) recebe mensagem da fila SQS.
2. Desencapsula envelope SNS e mapeia payload para `PaymentReceipt` (domínio).
3. Chama `SavePaymentReceiptUseCase.save(paymentReceipt)`.
4. `PaymentReceiptService` (application) invoca `PaymentReceiptRepositoryPort.save(...)`.
5. `PaymentReceiptDynamoDbAdapter` (adapter output) mapeia domínio → `PaymentReceiptEntity` e persiste via `DynamoDbOperations`.
6. Sucesso → `Consumer` faz `acknowledgement.acknowledge()`.

**Fluxo de erro — mensagem malformada (RF-6):**
1. `PaymentReceiptConsumer` falha ao desserializar/validar o payload.
2. Exceção capturada (`runCatching`), log de erro registrado.
3. Sem `acknowledge()` → mensagem retorna à fila / segue política de redrive (DLQ), como hoje.

**Fluxo de erro — falha ao persistir (RF-7):**
1. `PaymentReceiptDynamoDbAdapter` lança exceção ao salvar.
2. Exceção propaga até o `Consumer`, capturada por `runCatching`, log de erro registrado.
3. Sem `acknowledge()` → mesma semântica de reentrega atual.

**Fluxo GET não encontrado (RF-5):**
1. `PaymentReceiptController` chama `FindPaymentReceiptUseCase.findById(id)`.
2. `PaymentReceiptService` consulta `PaymentReceiptRepositoryPort.findByPk(id)`, retorna null.
3. Serviço lança `PaymentReceiptNotFoundException` (domínio).
4. `PaymentReceiptExceptionHandler` (adapter input, inalterado) converte para `404`.

## 7. Infraestrutura e Deploy
Nenhuma mudança de infraestrutura. Mesma imagem Docker, mesmas variáveis de ambiente
(`PAYMENT_RECEIPT_QUEUE_NAME`), mesmos stacks CDK (`sns-stack`, `payment-receipt-dynamodb-stack`,
`payment-receipt-service-stack`). A refatoração é inteiramente interna ao código-fonte do serviço.

## 8. Tratamento de Erros e Resiliência
Preserva integralmente o comportamento atual, agora com responsabilidades mais claras:
- Erros de infraestrutura (parse, DynamoDB) continuam sendo tratados nos adapters, nunca vazando
  detalhes de SDK/framework para o domínio.
- `PaymentReceiptNotFoundException` continua sendo uma exceção de domínio, tratada pelo
  `@ControllerAdvice` existente no adapter REST.
- Ack manual do SQS continua condicionado exclusivamente ao sucesso do caso de uso.

## 9. Considerações de Segurança
Sem mudanças. Nenhuma nova superfície de exposição, autenticação ou dado sensível introduzido pela
refatoração.

## 10. Estratégia de Testes
| Nível       | O que testar | Abordagem |
|-------------|-------------|-----------|
| Unitário    | Regras de negócio em `PaymentReceiptService` (domínio/application) | Mock das portas (`mock()`), sem dependência de Spring/AWS SDK |
| Unitário    | Mapeamento `PaymentReceiptMapper` (entidade ↔ domínio) | Testes diretos, sem mocks |
| Integração  | `PaymentReceiptConsumer` (adapter SQS) — RF-4, RF-6, RF-8 | `@SqsTest` + LocalStack, mantendo `PaymentReceiptConsumerIT` existente |
| Integração  | `PaymentReceiptDynamoDbAdapter` (adapter output) — RF-7 | TestContainers + LocalStack |
| E2E         | `GET /api/payment-receipt/{id}` — RF-5 | Testes de contrato existentes, sem alteração de asserts |

Todos os testes atuais devem continuar passando sem alteração de comportamento observável; novos testes
unitários cobrindo a lógica de domínio isolada devem ser adicionados como parte da entrega.

## 11. Riscos e Mitigações
| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Quebra silenciosa do contrato REST, afetando o grande cliente externo | Baixa | Alto | Manter testes de contrato/integração existentes intactos como gate de merge |
| Divergência entre `PaymentReceiptEntity` e `PaymentReceipt` (domínio) causando bug de mapeamento | Média | Médio | Testes unitários dedicados ao `PaymentReceiptMapper` cobrindo todos os campos, incluindo TTL |
| Branch longa gerar conflitos grandes na integração final | Média | Médio | Rebase periódico contra `master` durante o desenvolvimento da branch |

## 12. Registro de Decisões
| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Convenção de pacotes `domain` / `application/port` / `adapters/input,output` | `core`/`infrastructure`; camadas por feature sem hexagonal explícito | Convenção padrão e amplamente reconhecida de arquitetura hexagonal, facilita onboarding |
| Separar `PaymentReceipt` (domínio) de `PaymentReceiptEntity` (DynamoDB) | Manter uma única classe anotada com `@DynamoDbBean` | Necessário para desacoplar domínio do AWS SDK, objetivo central da refatoração |
| Refatorar um serviço por vez, começando pelo `payment-receipt-service` | Refatorar os dois serviços simultaneamente | Reduz risco e blast radius; permite validar a abordagem antes de aplicar ao `payment-service` |
| Entrega via branch longa, sem incrementos parciais em produção | Entregas incrementais com feature flag | Refatoração é puramente interna, sem mudança de comportamento externo, então não há necessidade de rollout controlado |
