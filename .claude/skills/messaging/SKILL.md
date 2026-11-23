---
name: messaging
description: Receita de mensageria com Kafka neste projeto, com evento de domínio, Transactional Outbox, tópico, @KafkaListener e teste ponta a ponta. Use ao publicar ou consumir um evento, criar um tópico, um producer ou um consumer Kafka, ou mexer no outbox.
---

# Mensageria (Kafka + Transactional Outbox)

O core nunca vê Kafka. Um caso de uso publica por uma porta de saída, o adapter grava no **outbox** na mesma transação, e o `OutboxRelay` (genérico, não precisa mudar) leva as mensagens ao Kafka. O consumo entra por um listener que chama uma porta de entrada. A entrega é **at-least-once**.

Fluxo modelo, ponta a ponta: `RequestWelcomeUseCaseImpl` → `OutboxUserEventPublisher` → `OutboxRelay` → `WelcomeRequestedListener` → `SendWelcomeUseCaseImpl`.

## Publicar um evento

1. **Evento** em `domain/`: um record com tipos de domínio e `occurredAt`, nomeado como fato (`...Requested`, `...Registered`).
2. **Porta:** adicione um método `publish(<Evento>)` em `application/port/out/UserEventPublisher` (ou numa porta nova, para outro contexto).
3. **Caso de uso** (skill `use-case`): ele chama a porta, e o bean dele usa `TransactionTemplate`. É isso que coloca o outbox na mesma transação da mudança de negócio. O `KafkaTemplate` fica fora do caso de uso.
4. **Tópico** em `infrastructure/config/MessagingConfig`:
   - uma constante `public static final String` com o nome `<contexto>.<evento-kebab>`;
   - um bean `NewTopic`, que o KafkaAdmin cria no startup.
5. **Adapter** em `adapter/out/outbox/OutboxUserEventPublisher`:
   - um record de mensagem com campos primitivos e `Instant` (esse é o contrato JSON);
   - `outbox.save(new OutboxJpaEntity(TOPICO, <id do agregado como chave>, json.writeValueAsString(msg), occurredAt))`;
   - a chave igual ao id do agregado mantém a ordem por agregado.

## Consumir um evento

6. **Listener** em `adapter/in/messaging/`: classe package-private com `@KafkaListener(topics = MessagingConfig.<TOPICO>)`.
   - Ela tem o **próprio** record de mensagem: o contrato é duplicado de propósito, um de cada lado.
   - Faz o parse com `JsonMapper`, reconstrói o evento de domínio e chama uma porta de entrada.
7. **Idempotência:** o mesmo evento pode chegar duas vezes. O caso de uso consumidor precisa produzir o mesmo efeito na repetição. Para efeitos não repetíveis (cobrança, e-mail real), registre os ids já processados.

## Contrato

Evolua o JSON só **adicionando** campos. Consumidores antigos ignoram campo desconhecido, mas quebram com campo renomeado ou removido. Para quebrar o contrato, crie um tópico novo.

## Testes

- **Unitário** do caso de uso publicador: uma porta fake `published::add` sobre uma `List`, verificando que o evento sai no caminho feliz e **não** sai nos caminhos alternativos.
- **HTTP e acesso:** em `SecurityTest`. Ali o rollback por teste faz o outbox nunca ser publicado, o que é esperado.
- **Ponta a ponta:** siga o modelo `WelcomeFlowTest`:
  - **sem** `@Transactional`, porque o relay só enxerga o outbox depois do commit;
  - `app.outbox.poll-interval=200ms`;
  - `@MockitoSpyBean` na porta de saída final;
  - `verify(porta, timeout(30_000))`;
  - dados com e-mail aleatório, porque o banco não é limpo.

## Pronto quando

O evento sai pelo outbox e o consumidor recebe, o que o teste ponta a ponta comprova. Além disso, `./mvnw test` passa e a seção "Mensageria" do README lista o tópico novo.
