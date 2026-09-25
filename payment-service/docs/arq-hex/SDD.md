# SDD: Refatoração do payment-service para Arquitetura Hexagonal

## 1. Contexto e Escopo
O `payment-service` cria, consulta e exclui pagamentos: persiste em DynamoDB (`payment`, `pk` UUID + `sk`
tipo do evento) e publica eventos no SNS `payment-event`. Este design cobre a reorganização interna em
`domain / application / adapters`, sem mudar contrato externo, infraestrutura ou stack. Difere do
`receipt-service` em dois pontos: o `payment-service` tem **dois adapters de saída** (DynamoDB e SNS) e três
casos de uso, e só uma entrada (REST).

A refatoração é feita **antes** da atualização de versões, então o código continua na stack atual (Kotlin
1.8.22, Spring Boot 3.1.1, Spring Cloud AWS 3.0.0, Jackson 2, JDK 17). Os trechos do `receipt-service` usados
como referência já estão na stack nova; onde diferem, este documento indica a variante da stack atual.

## 2. Visão Geral da Arquitetura

```
                 ┌──────────────────────────────────────────────┐
                 │                   DOMAIN                       │
                 │ Payment · EventType · PaymentEvent             │
                 │ PaymentNotFoundException                       │
                 │ PaymentDeletionNotAllowedException · TtlUtils  │
                 └───────────────────▲────────────────────────────┘
                                      │ usa
                 ┌────────────────────┴───────────────────────────┐
                 │                 APPLICATION                     │
                 │ port/input : Save / Find / DeletePaymentUseCase │
                 │ port/output: PaymentRepositoryPort              │
                 │              PaymentEventPublisherPort          │
                 │ PaymentService (implementa os 3 use cases)      │
                 └───▲───────────────────────────▲─────────────────┘
        chama        │                           │ implementa
  ┌──────────────────┴──────┐      ┌─────────────┴───────────────────┐
  │   ADAPTERS - INPUT       │      │        ADAPTERS - OUTPUT         │
  │ rest/PaymentController   │      │ dynamodb/PaymentDynamoDbAdapter  │
  │ rest/PaymentRequest      │      │          PaymentEntity           │
  │ rest/PaymentResponse     │      │          PaymentMapper           │
  │ rest/handler/*           │      │ sns/PaymentEventSnsAdapter       │
  └──────────────────────────┘      │     PaymentEventRequest          │
                                    └──────────────────────────────────┘
```

Dependências: `adapters → application → domain`. Domínio e aplicação não importam Spring web, AWS SDK,
Jackson nem Bean Validation. `@Service` e o `Logger` continuam permitidos, como no `receipt-service`.

Estrutura de código (mesmo padrão do `receipt`):

```
br/com/developers/
├── PaymentServiceApplication.kt
├── infra/dynamodb/DynamoDbConfiguration.kt   # PaymentEntity → tabela `payment`
└── payment/
    ├── domain/            Payment, EventType, PaymentEvent, TtlUtils,
    │                      PaymentNotFoundException, PaymentDeletionNotAllowedException
    ├── application/
    │   ├── PaymentService.kt
    │   └── port/{input,output}/
    └── adapters/
        ├── input/rest/    PaymentController, PaymentRequest (+CreditRequest),
        │                  PaymentResponse, handler/
        └── output/
            ├── dynamodb/  PaymentDynamoDbAdapter, PaymentEntity, PaymentMapper
            └── sns/       PaymentEventSnsAdapter, PaymentEventRequest
```

O pacote `event/` deixa de existir: o publisher vira adapter de saída e `PaymentEventRequest` vira DTO de
fio dentro dele.

## 3. Componentes e Responsabilidades
| Componente | Responsabilidade | Tecnologia |
|------------|-----------------|------------|
| `domain.Payment` | Modelo de domínio (`pk`, `sk`, `date`, `value`, `description`, `pixKeyCredit`, `ttl`) + factory que gera `pk` e define `sk` pela data | Kotlin puro |
| `domain.EventType` | `PROCESSED_PAYMENT`, `SCHEDULED_PAYMENT`, `DELETED_PAYMENT` | Kotlin puro |
| `domain.PaymentEvent` | Evento a publicar: `id`, `eventType`, `date`, `pixKeyCredit`, sem anotações Jackson | Kotlin puro |
| `domain.*Exception` | `PaymentNotFoundException` e `PaymentDeletionNotAllowedException` | Kotlin puro |
| `port.input.{Save,Find,Delete}PaymentUseCase` | Casos de uso | Interface |
| `port.output.PaymentRepositoryPort` | `findByPk`, `save`, `delete` | Interface |
| `port.output.PaymentEventPublisherPort` | `publish(event: PaymentEvent)` | Interface |
| `application.PaymentService` | Orquestra: regras de exclusão, montagem do evento, ordem persistir → publicar | Kotlin (`internal`, `@Service`) |
| `adapters.input.rest.PaymentController` | `/api/payment`, converte request → domínio e domínio → `PaymentResponse` | Spring Web |
| `adapters.input.rest.PaymentRequest` | Bean Validation + `@JsonProperty("credit")`, `("pix_key")` | Jakarta Validation, Jackson |
| `adapters.input.rest.PaymentResponse` | Corpo de resposta, com os campos do JSON atual | Kotlin |
| `adapters.input.rest.handler.*` | `@ControllerAdvice` (400/404/422/500) | Spring Web |
| `adapters.output.dynamodb.PaymentDynamoDbAdapter` | Implementa `PaymentRepositoryPort` sobre `DynamoDbTemplate` | Spring Cloud AWS DynamoDB |
| `adapters.output.dynamodb.PaymentEntity` / `PaymentMapper` | `@DynamoDbBean` (defaults em todos os campos, `ttl`) e conversão entidade ↔ domínio | AWS SDK Enhanced Client |
| `adapters.output.sns.PaymentEventSnsAdapter` | Implementa `PaymentEventPublisherPort`, define o header `event_type`, captura e loga falhas | Spring Cloud AWS SNS |
| `adapters.output.sns.PaymentEventRequest` | DTO de fio snake_case (`id`, `event_type`, `date`, `pix_key_credit`) | Jackson annotations |
| `infra.dynamodb.DynamoDbConfiguration` | `DynamoDbTableNameResolver`: `PaymentEntity` → `payment` | Spring Cloud AWS |

## 4. Modelo de Dados
Sem mudança de esquema. A tabela `payment` continua com `pk` (HASH) e `sk` (RANGE), mais `date`, `value`,
`description`, `pix_key_credit` e `ttl` (60 min). As anotações `@DynamoDb*` saem de `Payment` e vão para
`PaymentEntity`, com os mesmos atributos e defaults. `Payment` de domínio mantém `pk: UUID?` e `sk: String?`,
como hoje (paridade com o `receipt-service` e com os testes existentes). Tipar `sk` como `EventType` fica como
melhoria posterior.

Cuidado: sem o resolver, a classe `PaymentEntity` resolveria para a tabela `payment_entity`, que não existe.
Como o `receipt-service` já resolveu isso com `DynamoDbConfiguration`, replica-se o padrão. Na stack atual o
override é `fun <T> resolve(clazz: Class<T>): String`; `<T : Any>` é da versão 4 do Spring Cloud AWS (ver o
diff do commit `feb5c4c`) e só entra na atualização.

## 5. Design de API / Interface
Sem mudança de contrato.
- `POST /api/payment` → `201` + `Location: /api/payment/{pk}` + corpo | `400`
- `GET /api/payment/{id}` → `200` | `404`
- `DELETE /api/payment/{id}` → `204` | `422` (pagamento `PROCESSED_PAYMENT` ou inexistente)
- `GET /actuator/health`
- Erro: `{ "errors": [{ "message": "..." }] }`
- Evento SNS: `{id, event_type, date, pix_key_credit}` + atributo de mensagem `event_type`

**Ponto de atenção:** hoje o corpo do `POST` e do `GET` é o `Payment` serializado direto pelo Jackson padrão
do Boot, sem naming strategy. O `PaymentResponse` precisa reproduzir isso exatamente, e o RF-1 fixa o formato
antes de qualquer alteração. O formato exato ainda não foi observado por um teste e não deve ser assumido.

## 6. Fluxos de Sequência

**Criar pagamento**
1. `PaymentController` valida o `PaymentRequest` (`@Valid`) e cria `Payment` pela factory do domínio (`pk`
   novo; `sk` = `PROCESSED_PAYMENT` se `date == hoje`, senão `SCHEDULED_PAYMENT`).
2. `SavePaymentUseCase.save(payment)` → `PaymentService`.
3. `PaymentRepositoryPort.save(payment)` → `PaymentDynamoDbAdapter` mapeia para `PaymentEntity` e persiste.
4. `PaymentService` monta o `PaymentEvent` com `eventType = payment.sk` e chama
   `PaymentEventPublisherPort.publish(event)`.
5. `PaymentEventSnsAdapter` converte para `PaymentEventRequest`, seta o header `event_type` e chama
   `SnsTemplate`.
6. `201` + `Location`.

**Excluir pagamento**
1. `DeletePaymentUseCase.delete(id)` → `PaymentService` chama `findByPk(id)`.
2. Inexistente → `PaymentDeletionNotAllowedException("Payment $id not found")` → `422`.
3. `sk == PROCESSED_PAYMENT` → `PaymentDeletionNotAllowedException("Payment processed $id can't be change")`
   → `422`. Nada é apagado nem publicado.
4. Caso contrário: `delete(payment)` e publica `PaymentEvent(eventType = DELETED_PAYMENT)` → `204`.

**Consultar pagamento inexistente**
`findByPk` devolve `null` → `PaymentNotFoundException("Payment $id not found")` → `404`.

**Falha de publicação (comportamento atual preservado)**
`SnsTemplate` lança exceção → o adapter captura e loga; o pagamento continua salvo, `201`/`204` são devolvidos
e o evento não sai. O fluxo permanece idêntico ao de hoje.

## 7. Infraestrutura e Deploy
Sem mudança. Mesma imagem, variável `PAYMENT_TOPIC_NAME`, mesma tabela, mesmo tópico e mesmos stacks CDK.
`payment.topic.name` continua lido com `@Value` dentro do adapter SNS. A entrega é uma única branch com PR,
sem rollout controlado, já que não há mudança externa.

## 8. Tratamento de Erros e Resiliência
- Exceções de negócio são de domínio e continuam mapeadas no `@ControllerAdvice` do adapter REST.
- Erros de infraestrutura (DynamoDB, SNS) ficam nos adapters. O de SNS continua engolido (fora de escopo); o
  de DynamoDB propaga e vira `500`.
- Um erro no mapeamento entidade ↔ domínio seria silencioso em produção até o primeiro uso. Por isso o
  `PaymentMapperTest` cobre todos os campos, incluindo `ttl`.

## 9. Considerações de Segurança
Sem mudança. O domínio deixa de ser serializado direto na resposta, o que reduz o risco de expor por acidente
um campo novo da entidade.

## 10. Estratégia de Testes
| Nível | O que testar | Abordagem |
|-------|--------------|-----------|
| Caracterização (antes) | Contrato REST e payload SNS atuais (RF-1) | `PaymentControllerIT` com `TestRestTemplate` e teste de payload, ambos com LocalStack 0.14.3 e `init.sh` (que ganha fila SQS assinada no tópico); rodar antes e depois |
| Unitário | `PaymentService` (save, find, delete, regras 422) | `mock()` das duas portas; `argumentCaptor<PaymentEvent>` no lugar do `PaymentEventRequest` |
| Unitário | Factory do domínio (`sk` por data, `pk` gerado) | Sem mocks; migra a lógica do `PaymentRequestTest` |
| Unitário | `PaymentMapper`, `TtlUtils`, `PaymentEventSnsAdapter` | Mapper com todos os campos; adapter com `SnsTemplate` mockado (migra o `PaymentEventPublisherTest`) |
| Integração | `PaymentDynamoDbAdapter` | LocalStack; mesmos cenários do `PaymentRepositoryIT`; valida também que a tabela resolve para `payment` |
| E2E manual | 201/404/400/204/422 + evento em `payment-receipt` | `docker-compose up` + perfil `local` nos dois serviços |

Comandos: `./mvnw test` e `./mvnw test -Dtest=<Nome>IT`. O fluxo `feature-flow` (test-writer →
code-reviewer → developer) roda no fim.

## 11. Riscos e Mitigações
| Risco | Prob. | Impacto | Mitigação |
|-------|-------|---------|-----------|
| O corpo de `POST`/`GET` muda (por exemplo `pix_key_credit` em vez de `pixKeyCredit`, ou `ttl` some) ao trocar a entidade por `PaymentResponse` | Média | Alto | RF-1: caracterização antes de mover código; `PaymentControllerIT` como gate |
| Tabela resolve para `payment_entity` | Alta se esquecido | Alto | `DynamoDbConfiguration` + IT do adapter |
| Payload SNS muda; a falha é engolida pelo publisher | Baixa | Alto | Teste de payload do RF-1 |
| Divergência entre `PaymentEntity` e `Payment` | Média | Médio | `PaymentMapperTest` completo |
| Regra do `sk` passa a divergir ao ser movida do DTO | Baixa | Médio | Teste de caracterização (data de hoje vs. futura) antes e depois da mudança |
| Conflitos de merge com o `CLAUDE.md` raiz já alterado | Média | Baixo | Commitar ou stashear antes; `docs-sync` edita por cima |
| Push acidental de imagem | Baixa | Alto | Nunca rodar `verify`, `install` ou `deploy` |
| Refatoração misturada com a atualização de versões | Média | Médio | Fazer uma de cada vez; PRs separados |
| A atualização posterior reescreve pontos dos adapters (Jackson 3, resolver `<T : Any>`, Testcontainers/LocalStack dos ITs) | Alta | Baixo | Framework confinado nos adapters e em `infra/`; a atualização mexe em poucos arquivos |

## 12. Registro de Decisões
| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Mesmo layout do `receipt-service` (`domain` / `application/port` / `adapters/input,output`) | `core`/`infrastructure` | Convenção única entre os serviços |
| Entidade DynamoDB separada do domínio, com mapper | Manter `Payment` anotada | Objetivo central: desacoplar o domínio do AWS SDK |
| `PaymentResponse` como DTO de saída | Serializar o `Payment` de domínio | Não amarra o contrato REST ao modelo interno; exige teste de caracterização |
| Porta de saída própria para eventos (`PaymentEventPublisherPort`) | Chamar o `SnsTemplate` no serviço | O `receipt-service` não tem esse caso; aqui o publish é parte do fluxo de negócio e precisa ser mockável |
| `PaymentEvent` de domínio + `PaymentEventRequest` como DTO de fio | Reusar `PaymentEventRequest` no serviço | Tira o Jackson e o snake_case do domínio e da aplicação |
| Manter `sk` e `pk` como `String`/`UUID?` e a semântica do publisher | Tipar `sk` como enum; propagar falha de publicação | Paridade de comportamento; melhorias ficam para depois |
| Regra de `sk`/`pk` em factory do domínio | Deixar em `toPayment()` no adapter | É regra de negócio; ficará testável sem Spring (pendente da pergunta 3 do PRD) |
| Testes de caracterização antes de refatorar | Confiar nos testes atuais | Não há teste de controller nem de handler; a mudança da resposta seria silenciosa |
| Serviço sem sufixo `Impl`; adapters `@Component` | `Impl` e `@Repository` (convenção atual) | Alinhar com o `receipt-service` |
