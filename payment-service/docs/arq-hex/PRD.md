# PRD: Refatoração do payment-service para Arquitetura Hexagonal

## 1. Visão Geral
Reorganizar o `payment-service` em arquitetura hexagonal (ports & adapters), como já foi feito no
`payment-receipt-service`. O objetivo é isolar a regra de negócio de Spring, AWS SDK (DynamoDB, SNS) e
Jackson, sem alterar nenhum comportamento externo.

## 2. Problema
Hoje o serviço é em camadas (`controller → service → repository + publisher`) e o negócio está acoplado à
infraestrutura:
- `Payment` é ao mesmo tempo entidade `@DynamoDbBean`, modelo de negócio e corpo de resposta REST (o
  controller devolve `ResponseEntity<Payment>`).
- `PaymentServiceImpl` depende de classes concretas (`PaymentRepository` sobre `DynamoDbTemplate`,
  `PaymentEventPublisher` sobre `SnsTemplate`), não de contratos.
- Regras de negócio estão no DTO: `PaymentRequest.toPayment()` gera o `pk` e decide `PROCESSED_PAYMENT` ou
  `SCHEDULED_PAYMENT` conforme a data.
- Os dois serviços têm arquiteturas diferentes, o que dificulta a manutenção e o onboarding.

## 3. Objetivos e Métricas de Sucesso
| Objetivo | Métrica | Meta |
|----------|---------|------|
| Preservar o contrato REST | Status, corpo (nomes e tipos dos campos), header `Location`, formato de erro | Idêntico ao atual, verificado por teste de caracterização escrito antes da refatoração |
| Preservar o contrato do evento SNS | Payload `{id, event_type, date, pix_key_credit}` + atributo `event_type` | Idêntico |
| Isolar o domínio | Imports proibidos em `domain` e `application` (`org.springframework.web`, `software.amazon`, `io.awspring`, `jakarta.validation`, `com.fasterxml.jackson`) | Zero ocorrências, verificável com `grep` |
| Manter testes existentes | Unitários + `PaymentRepositoryIT` | Passando. Só mudam imports, pacotes e tipos |
| Convenção única | Estrutura de pacotes e nomes | Igual à do `receipt-service` |

## 4. Não-Objetivos
- Não altera endpoints, regras de negócio, tabela DynamoDB, tópico SNS, CDK ou `docker-compose.yml`.
- Não altera o comportamento do publisher, que captura e loga falhas de publicação (dívida conhecida, fora
  de escopo).
- Não altera o `payment-receipt-service`.
- Não faz a atualização de versões (Spring Boot 4 / JDK 25); ela vem depois desta refatoração e tem
  PRD/SDD próprio.
- Não tipa `sk` como enum nem troca o comportamento de `DELETE` de pagamento inexistente (continua `422`).
- Não roda `./mvnw verify`, `install` ou `deploy` (push para o Docker Hub).

## 5. Histórias de Usuário
- Como desenvolvedor, quero testar a regra de negócio (pagamento processado não pode ser excluído, tipo do
  evento por data) sem Spring, DynamoDB ou SNS, para ter testes rápidos e focados.
- Como desenvolvedor, quero portas para persistência e publicação de eventos, para trocar ou mockar esses
  adapters sem tocar no domínio.
- Como cliente da API, quero que a refatoração seja transparente.
- Como consumidor do evento `payment-event`, quero o payload inalterado.

## 6. Requisitos Funcionais

### RF-1: Rede de segurança antes de mover código
O `payment-service` não tem teste de `PaymentController` nem de `PaymentExceptionHandler`. Antes da
refatoração, escrever testes de caracterização do contrato atual:
- `PaymentControllerIT`, no padrão do `PaymentReceiptControllerIT`, cobrindo `POST` (201 + `Location` +
  corpo), `GET` (200 / 404), `DELETE` (204 / 422 nos dois casos) e `POST` inválido (400 com formato de erro).
- Teste do payload SNS publicado (topic → fila → leitura no LocalStack) e do header `event_type`.

Na stack atual isso usa `TestRestTemplate` (já em `spring-boot-starter-test`) e o LocalStack 0.14.3 do
`PaymentRepositoryIT`. Para o teste de payload, o `init.sh` ganha uma fila SQS assinada no tópico
`payment-event` (hoje ele só cria a tabela e o tópico).

**Aceite:** os testes passam na estrutura atual e passam de novo, sem alteração, no final. O corpo de
resposta atual é o `Payment` serializado; o teste fixa exatamente o que for observado.

### RF-2: Camadas e pacotes
Estrutura `payment/domain`, `payment/application/port/{input,output}` e `payment/adapters/{input,output}`,
como no `receipt-service`, mantendo `br.com.developers` como base.
**Aceite:** dependências apontam para dentro (`adapters → application → domain`) e o grep do RF-3 dá zero.

### RF-3: Domínio puro
`Payment` (plain data class, sem `@DynamoDb*`), `EventType`, `PaymentEvent`, `PaymentNotFoundException`,
`PaymentDeletionNotAllowedException` e `ttlOf60Minutes()` em `domain`. A regra "PROCESSED se a data é hoje,
senão SCHEDULED" e a geração do `pk` saem de `PaymentRequest.toPayment()` e passam para uma factory do
domínio.
**Aceite:** a regra é testada em teste unitário sem Spring.

### RF-4: Portas e serviço de aplicação
- Entrada: `SavePaymentUseCase`, `FindPaymentUseCase`, `DeletePaymentUseCase`.
- Saída: `PaymentRepositoryPort` (`findByPk`, `save`, `delete`) e `PaymentEventPublisherPort` (`publish`).
- `PaymentService` implementa os três casos de uso e depende só das portas de saída. Não tem sufixo `Impl`,
  como no `receipt-service`.

**Aceite:** as regras atuais são preservadas: `save` persiste e publica; `delete` de pagamento inexistente
ou `PROCESSED_PAYMENT` gera `PaymentDeletionNotAllowedException` sem apagar nem publicar; `findById`
inexistente gera `PaymentNotFoundException`.

### RF-5: Adapters de saída
- `adapters/output/dynamodb`: `PaymentDynamoDbAdapter`, `PaymentEntity` (`@DynamoDbBean`, tabela `payment`,
  com `pk` + `sk` e `ttl`) e `PaymentMapper` (`toEntity` / `toDomain`).
- Configuração de nome de tabela (`PaymentEntity` → `payment`), como o `DynamoDbConfiguration` do
  `receipt-service`, com a assinatura do Spring Cloud AWS 3.0.0 (`fun <T> resolve(clazz: Class<T>)`). A
  variante `<T : Any>` do `receipt-service` é da versão 4 e só entra na atualização.
- `adapters/output/sns`: `PaymentEventSnsAdapter` (antigo `PaymentEventPublisher`, mesmo tratamento de
  falha) e `PaymentEventRequest` (DTO de fio com `@JsonProperty`).

**Aceite:** `PaymentDynamoDbAdapterIT` cobre save, find e delete com os mesmos cenários do
`PaymentRepositoryIT`; o teste do adapter SNS cobre payload e header.

### RF-6: Adapter de entrada REST
`PaymentController`, `PaymentRequest`/`CreditRequest` (com Bean Validation), `PaymentResponse` (corpo, com os
mesmos campos do JSON atual) e `handler/` (`PaymentExceptionHandler`, `ErrorResponse`,
`ErrorMessageResponse`) em `adapters/input/rest`. O controller chama apenas os casos de uso.
**Aceite:** o `PaymentControllerIT` do RF-1 passa sem alteração.

### RF-7: Testes reorganizados
`PaymentServiceTest` passa a usar mocks das portas. Ficam novos `PaymentMapperTest` e um teste da factory do
domínio. `PaymentRequestTest`, `PaymentEventPublisherTest`, `TtlUtilsTest` e o IT do adapter DynamoDB vão
para os pacotes espelhados. Só mudam tipos e pacotes; os cenários e a essência dos asserts continuam.

### RF-8: Documentação
Atualizar `payment-service/CLAUDE.md` (estrutura, regras de arquitetura, convenções), `CLAUDE.md` raiz
(linha "Architecture" da tabela, "Event Flow", "Repository Structure") e `README.md`.

## 7. Requisitos Não-Funcionais
| Área | Requisito |
|------|-----------|
| Performance | Sem regressão perceptível; o mapper adiciona uma cópia de objeto por operação, considerada irrelevante |
| Disponibilidade | Sem mudança de topologia (ECS Fargate 1–10 tasks); `/actuator/health` preservado |
| Segurança | Sem nova superfície, credenciais ou dados sensíveis |
| Conformidade | Sem APIs `@Deprecated` (regra do projeto); se inevitável, sinalizar explicitamente |
| Manutenibilidade | Injeção por construtor; adapters `@Component`; logging com `LoggerFactory.getLogger(javaClass)` |

## 8. Restrições e Premissas
- Esta refatoração é feita **antes** da atualização para Spring Boot 4 / JDK 25. A stack atual é mantida:
  Kotlin 1.8.22, Spring Boot 3.1.1, Spring Cloud AWS 3.0.0, Jackson 2, build com JDK 17
  (`JAVA_HOME=$(/usr/libexec/java_home -v 17)`) e LocalStack 0.14.3 nos ITs. Sem `brew`.
- Proibido `verify`, `install` e `deploy`.
- Branch curta com PR; commits `refactor:` e `test:`; usar `git mv` para preservar o histórico dos arquivos
  que só mudam de pacote.
- O `CLAUDE.md` raiz pode ter alterações não commitadas. O passo de docs deve editar por cima delas, sem
  sobrescrevê-las.
- Consequência da ordem: a atualização de versões, feita depois, vai tocar de novo os adapters e as
  configurações (pacotes do Jackson 3, assinatura do resolver de tabela, Testcontainers e LocalStack dos
  ITs). Por isso o framework fica confinado nos adapters, o que limita esse retrabalho a eles.

## 9. Perguntas em Aberto
| # | Pergunta | Responsável | Prazo |
|---|----------|-------------|-------|
| 1 | ~~Esta refatoração roda antes ou depois da atualização para Boot 4 / JDK 25?~~ **Decidido: antes.** A stack atual é mantida (ver seção 8) | Igor | Resolvida |
| 2 | O corpo de `POST`/`GET` é hoje a entidade serializada. Confirma criar `PaymentResponse` (recomendado) em vez de expor o `Payment` de domínio direto? | Igor | Início do RF-6 |
| 3 | Manter a regra de `sk` e `pk` em factory do domínio (recomendado) ou deixar em `toPayment()` no adapter, como está hoje? | Igor | Início do RF-3 |
| 4 | Adicionar um teste ArchUnit para impor a regra de dependências (novo dependency de teste) ou manter só o `grep` / `code-reviewer`? | Igor | Opcional |
