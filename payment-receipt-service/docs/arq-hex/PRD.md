# PRD: Refatoração do payment-receipt-service para Arquitetura Hexagonal

## 1. Visão Geral
Refatorar o payment-receipt-service para arquitetura hexagonal (ports & adapters), isolando a lógica de
negócio de frameworks e infraestrutura (Spring, AWS SDK), sem alterar nenhum comportamento externo
observável do serviço.

## 2. Problema
Hoje a lógica de negócio do payment-receipt-service está acoplada diretamente a frameworks e SDKs
(Spring, AWS SQS/DynamoDB SDK), o que traz três dificuldades concretas para o time de desenvolvimento:
(1) dificuldade de testar a lógica de negócio isoladamente do SQS e do DynamoDB; (2) acoplamento forte
que dificulta a manutenção e evolução do código; (3) dificuldade de trocar ou mockar adapters de
infraestrutura em testes e futuras migrações tecnológicas.

## 3. Objetivos e Métricas de Sucesso
| Objetivo | Métrica | Meta |
|----------|---------|------|
| Preservar comportamento externo | Testes existentes (unitários + integração) | 100% mantidos e passando sem alteração de asserts de comportamento |
| Aumentar testabilidade do domínio | Novos testes unitários da lógica de domínio pura (sem mocks de framework) | Cobrir os casos de uso e casos extremos mapeados (RF-4 a RF-7) |
| Zero impacto ao cliente externo | Contrato da API REST (`GET /api/payment-receipt/{id}`) | Sem nenhuma mudança de request/response/status codes |

## 4. Não-Objetivos
- Não altera o contrato da API REST exposta (`GET /api/payment-receipt/{id}`, `GET /actuator/health`).
- Não troca o banco de dados (permanece DynamoDB) nem o broker de mensageria (permanece SQS/SNS).
- Não altera a infraestrutura AWS CDK (filas, tópicos, tabelas, roles).
- Não inclui o `payment-service` — será tratado em uma refatoração futura e separada.
- Não introduz novos requisitos não-funcionais de performance/disponibilidade.

## 5. Histórias de Usuário
- Como desenvolvedor do time, quero que a lógica de negócio do payment-receipt-service seja testável
  sem depender de Spring ou do AWS SDK, para que eu possa escrever testes unitários rápidos e focados.
- Como desenvolvedor do time, quero portas (interfaces) bem definidas para SQS e DynamoDB, para que eu
  possa trocar ou mockar esses adapters sem tocar na lógica de domínio.
- Como grande cliente externo que consome `GET /api/payment-receipt/{id}`, quero que a refatoração
  interna seja completamente transparente, para que minha integração continue funcionando sem nenhuma
  mudança.

## 6. Requisitos Funcionais

### RF-1: Reorganização em camadas hexagonais
O código deve ser reorganizado em domínio (regras de negócio puras), aplicação (casos de uso/portas) e
adapters (entrada: REST e SQS; saída: DynamoDB), seguindo a convenção de nomenclatura padrão
`domain` / `application` (com subpasta `port`) / `adapters` (com subpastas `input`/`output`).

### RF-2: Porta de saída para persistência (DynamoDB)
Deve existir uma interface de porta de saída (ex: `PaymentReceiptRepositoryPort`) implementada por um
adapter DynamoDB, desacoplando o domínio do `DynamoDbOperations`/`DynamoDbTemplate`.

### RF-3: Portas de entrada (casos de uso)
Deve existir interfaces de caso de uso (ex: `SavePaymentReceiptUseCase`, `FindPaymentReceiptUseCase`)
implementadas pelo serviço de aplicação, consumidas pelos adapters de entrada (REST controller e SQS
listener) — nenhum deles deve chamar o repositório diretamente.

### RF-4: Preservação do fluxo de consumo SQS
O adapter de entrada SQS deve continuar desencapsulando o envelope SNS, convertendo o payload para o
modelo de domínio e delegando ao caso de uso de salvar, com o mesmo comportamento de acknowledgement
manual (ack apenas em caso de sucesso) já existente.

### RF-5: Preservação do endpoint REST
O adapter de entrada REST deve manter o endpoint `GET /api/payment-receipt/{id}` com o mesmo contrato de
request/response e o mesmo tratamento de erro (`PaymentReceiptNotFoundException` → 404).

### RF-6: Tratamento de mensagem SQS malformada
O comportamento atual de falha de validação/parse de mensagem (log de erro, sem ack, permitindo
reentrega/DLQ) deve ser preservado exatamente.

### RF-7: Tratamento de erro ao persistir no DynamoDB
Falhas ao salvar/atualizar no DynamoDB devem continuar sendo tratadas da mesma forma que hoje (log de
erro, sem ack da mensagem SQS).

### RF-8: Idempotência de reprocessamento
O comportamento atual diante de mensagens duplicadas (reprocessamento por retry/reentrega do SQS) deve
ser preservado sem alteração de semântica.

## 7. Requisitos Não-Funcionais
| Área          | Requisito |
|---------------|-----------|
| Performance   | Sem novas metas; manter os requisitos implícitos atuais |
| Disponibilidade | Sem novas metas; manter os requisitos implícitos atuais |
| Segurança     | Nenhuma mudança de superfície de exposição/autenticação |
| Conformidade  | Nenhuma mudança de requisito de conformidade |

## 8. Restrições e Premissas
- A refatoração será feita em uma branch longa e entregue de forma completa (não incremental), sem
  necessidade de feature flags ou rollout gradual.
- Refatoração de um serviço por vez: `payment-receipt-service` primeiro; `payment-service` fica para uma
  iniciativa futura e separada.
- Premissa: stack tecnológica (Kotlin, Spring Boot 3.1.1, Spring Cloud AWS 3.0.0) permanece a mesma.

## 9. Perguntas em Aberto
| # | Pergunta | Responsável | Prazo |
|---|----------|-------------|-------|
| 1 | Quem revisa/aprova a branch antes do merge final? | Time / a definir | A definir |
| 2 | A refatoração do `payment-service` será uma iniciativa separada — quando será priorizada? | Igor | A definir |
