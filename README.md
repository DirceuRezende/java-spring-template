# user-template — Hexagonal com Java 25 + Spring Boot 4

Template de referência com um agregado `User`, autenticação JWT e mensageria com Kafka (Transactional Outbox).

```bash
docker compose up -d                                   # Postgres 17 (5432) + Kafka (9092)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # app com segredo JWT e admin de dev
./mvnw test                                             # inclui ArchitectureTest (quebra o build se a regra de dependência for violada)
```

## Banco de dados

PostgreSQL em todos os ambientes. O `docker-compose.yml` sobe o banco de desenvolvimento (`postgres:17-alpine`, a mesma imagem dos testes) com volume persistente `pgdata`. `docker compose down` para; `docker compose down -v` apaga os dados.

A conexão vem de variáveis de ambiente; os defaults apontam para o banco do compose:

| Variável | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/users` |
| `DB_USERNAME` | `users` |
| `DB_PASSWORD` | `users` |

### Migrations (Flyway)

O schema é versionado pelo **Flyway** em `src/main/resources/db/migration` e aplicado automaticamente no startup. O Hibernate roda com `ddl-auto: validate`: só confere se as entidades batem com o banco, e a app não sobe se divergirem.

- Mudou uma entidade? Crie a próxima migration: `V2__descricao.sql`, `V3__...` (dois underscores).
- Nunca edite uma migration já aplicada: o Flyway detecta a mudança de checksum e recusa subir.
- Os testes de integração aplicam as mesmas migrations no Postgres do Testcontainers, então uma migration quebrada falha o `./mvnw test`.

## Endpoints

Documentação interativa (springdoc, pública em todos os profiles): **Swagger UI** em `http://localhost:8080/swagger-ui.html` e spec OpenAPI em `/v3/api-docs`. Para rotas protegidas: faça o login, clique em **Authorize** e cole o `accessToken` (sem `Bearer`).

Collection do Postman em `postman/user-template.postman_collection.json` (importe no Postman; o Login salva os tokens sozinho).

| Método | Rota | Resposta |
|---|---|---|
| POST | `/api/users` | 201 / 409 email duplicado |
| GET | `/api/users?page=0&size=20` | 200 paginado (size 1..100) |
| GET | `/api/users/{id}` | 200 / 404 |
| PUT | `/api/users/{id}` | 200 / 404 (altera só o nome) |
| DELETE | `/api/users/{id}` | 204 / 404 (soft delete: `active=false`) |
| POST | `/api/users/{id}/welcome` | 202 / 404 (assíncrono, via Kafka; usuário inativo = 404) |

## Estrutura

```
br.com.dirceurezende.template
├── TemplateApplication.java
└── user/                                   ← feature (bounded context)
    ├── domain/                             ← Java puro. Zero Spring/JPA/Jackson.
    │   ├── User.java                       agregado (ID gerado aqui, não no banco)
    │   ├── UserId, Email, PasswordHash     value objects (records)
    │   ├── Role.java
    │   └── DomainValidationException.java
    ├── application/                        ← casos de uso. Também sem Spring.
    │   ├── port/in/                        o que o mundo pode pedir
    │   │   ├── RegisterUserUseCase + Command + Result (sealed)
    │   │   ├── GetUserUseCase + UserView
    │   │   └── LoadUserCredentialsUseCase + UserCredentials   ← gancho p/ Spring Security
    │   ├── port/out/                       o que o core precisa do mundo
    │   │   ├── UserRepository
    │   │   └── PasswordHasher
    │   └── usecase/                        implementações dos use cases (POJOs)
    └── infrastructure/
        ├── adapter/in/web/                 Controller, Request/Response, ProblemDetail
        ├── adapter/in/messaging/           @KafkaListener → caso de uso
        ├── adapter/out/outbox/             outbox (JPA) + relay para o Kafka
        ├── adapter/out/notification/       WelcomeNotifier (log)
        ├── adapter/out/persistence/        JpaEntity, Spring Data, Mapper, Adapter
        ├── adapter/out/security/           PasswordHasher via PasswordEncoder
        └── config/                         monta os beans + transações (TransactionTemplate)
```

### Decisões

| Decisão | Por quê |
|---|---|
| Application sem `@Service`/`@Transactional` | Casos de uso testáveis sem Spring; beans e transação montados em `UserBeansConfig`. Se preferir pragmatismo, anote o service e remova `org.springframework..` da regra do ArchUnit para `application` — decisão consciente. |
| `RegisterUserResult` sealed | Email duplicado é resultado esperado, não exceção. O `switch` no controller é exaustivo: novo caso = erro de compilação. |
| `PasswordHash` no domínio, `PasswordHasher` como porta | Domínio nunca vê senha crua. O mesmo `PasswordEncoder` serve cadastro e login. |
| `UserView` / `UserCredentials` | O agregado não vaza para fora da application; credenciais têm porta própria para a autenticação. |
| `toString()` mascarado em Command/Request/Hash | Senha nunca vai parar em log. |
| `@NullMarked` em domain/application | Null-safety JSpecify, padrão do Spring 7. |
| Flyway + `ddl-auto: validate` | Schema versionado e revisável em SQL; o Hibernate nunca altera o banco, só valida o mapeamento. |

## Segurança (JWT + refresh token)

Rodar local: `docker compose up -d` e `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (segredo de dev + admin `admin@example.com` / `admin12345`, só nesse profile). Fora do dev, `JWT_SECRET` (≥ 32 bytes) é obrigatório e o primeiro admin é provisionado por fora.

| Rota | Quem |
|---|---|
| `POST /api/users`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` | público |
| `GET /swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | público |
| `GET /api/users` (lista) | só ADMIN |
| `GET/PUT/DELETE /api/users/{id}`, `POST /api/users/{id}/welcome` | ADMIN, ou o próprio usuário |

- **Login** devolve `accessToken` (JWT HS256, 15 min, `sub` = id, claim `roles`) e `refreshToken` (opaco, 7 dias, guardado só como SHA-256 em `refresh_tokens`). Use `Authorization: Bearer <accessToken>`.
- **Refresh** rotaciona: cada refresh token vale uma vez. **Logout** revoga o refresh token enviado. Excluir (desativar) um usuário revoga todos os refresh tokens dele; inativo não loga nem renova.
- Autorização por papel/dono é `@PreAuthorize` no controller; `application` segue sem Spring (os casos de uso emitem tokens pela porta `AccessTokenIssuer`).

**Limites conhecidos:** o access token continua válido até expirar (≤ 15 min) após logout/exclusão; sem detecção de reuso de refresh token, sem rate limit/lockout no login e sem limpeza de refresh tokens expirados.

## Mensageria (Kafka + Transactional Outbox)

Exemplo de producer **e** consumer sem que o core conheça o Kafka: `domain` e `application` só veem portas (o `ArchitectureTest` proíbe `org.apache.kafka..` nessas camadas).

```
POST /api/users/{id}/welcome
  └─ RequestWelcomeUseCase ──(porta out UserEventPublisher)──► OutboxUserEventPublisher ─► tabela outbox   (mesma transação) → 202
OutboxRelay (@Scheduled, 1s) ─► outbox pendente (FOR UPDATE SKIP LOCKED) ─► KafkaTemplate ─► tópico user.welcome-requested
@KafkaListener WelcomeRequestedListener ─► SendWelcomeUseCase ─(porta out WelcomeNotifier)─► LoggingWelcomeNotifier (log)
```

| Peça | Onde |
|---|---|
| Evento `WelcomeRequested` | `domain/` |
| Portas `UserEventPublisher`, `WelcomeNotifier`, `RequestWelcomeUseCase`, `SendWelcomeUseCase` | `application/port/` |
| Outbox (entidade, repositório, publisher, relay) | `infrastructure/adapter/out/outbox/` |
| Listener Kafka (adapter de entrada) | `infrastructure/adapter/in/messaging/` |
| Notifier por log (troque por e-mail real) | `infrastructure/adapter/out/notification/` |
| Tópico + `@EnableScheduling` | `infrastructure/config/MessagingConfig` |

**Por que outbox?** Gravar no banco e publicar no Kafka são duas operações sem transação comum. Publicando direto, um crash entre o commit e o envio perde o evento (ou o evento sai e o commit falha). Com outbox o evento é gravado na mesma transação; o relay publica depois e só marca `published_at` após o ack do broker.

**Entrega at-least-once:** se o relay cair depois do envio e antes do commit, a mensagem é reenviada. O consumidor precisa tolerar duplicatas (o notifier por log tolera; um envio de e-mail real deveria registrar o que já processou).

Configuração: `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`, o Kafka do compose) e `app.outbox.poll-interval` (default `1s`). Ver as mensagens:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic user.welcome-requested --from-beginning
```

**Limites conhecidos:** sem tabela de idempotência no consumidor, sem dead-letter topic (o error handler padrão tenta 10x e descarta), sem limpeza das linhas já publicadas do outbox e sem schema registry (contrato é JSON, com um record de cada lado).

## Testes

Unitários (fakes) rodam sem infraestrutura. Os de integração usam **Testcontainers com PostgreSQL e Kafka**: precisam do Docker rodando. `WelcomeFlowTest` cobre o caminho completo HTTP → outbox → Kafka → listener (sem `@Transactional`, pois o relay só enxerga o outbox depois do commit).

## Pontos em aberto para um projeto real

- **Corrida no cadastro**: `existsByEmail` + `save` não é atômico. A constraint `unique` no banco garante; traduza `DataIntegrityViolationException` no adapter para `EmailAlreadyTaken`.
- **Lock otimista**: adicionar `@Version` na entidade e versão no agregado quando houver updates concorrentes.
- **Spring Modulith**: ao surgir a segunda feature (`orders/`, `auth/`…), use `ApplicationModules.of(TemplateApplication.class).verify()` para validar fronteiras entre features.
