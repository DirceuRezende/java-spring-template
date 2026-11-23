# user-template

Template de arquitetura **hexagonal** em Java 25 + Spring Boot 4.1. Feature única hoje: `user/` (bounded context), base `br.com.dirceurezende.template`.

## Comandos

```bash
docker compose up -d                                    # Postgres + Kafka de dev
./mvnw test                                             # precisa do Docker (Testcontainers)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # JWT de dev + admin admin@example.com / admin12345
```

Uma tarefa está pronta quando `./mvnw test` passa por inteiro, incluindo o `ArchitectureTest`.

## Regra de dependência

`infrastructure → application → domain`, verificada pelo `ArchitectureTest`.

- `domain/` e `application/` são Java puro: sem Spring, Jakarta, Jackson, Hibernate ou Kafka. Também sem `@Service` e sem `@Transactional`.
- Os casos de uso viram beans, e ganham transação, em `infrastructure/config/UserBeansConfig`.
- O mundo externo entra e sai só por portas: `application/port/in` para o que pedem ao core, `application/port/out` para o que o core precisa do mundo.
- Pacote novo em `domain/` ou `application/` ganha um `package-info.java` com `@NullMarked`, porque a anotação vale só para o pacote exato.

## Convenções

- Identificadores em inglês; comentários, mensagens de erro e docs em português.
- Classes de adapter são package-private e implementam uma porta.
- Commands e Requests que carregam segredo sobrescrevem `toString()` para mascarar o valor (ex.: `RegisterUserCommand`).
- Exemplo novo (cache, job, mensageria...) ganha endpoint e caso de uso próprios, sem mexer nos fluxos existentes.

## Pegadinhas do Boot 4

- Os starters são por módulo. O `@WithMockUser` só funciona no MockMvc com `spring-boot-starter-security-test`; o Flyway vem por `spring-boot-starter-flyway`.
- O Jackson é o 3 (`tools.jackson.databind.json.JsonMapper`, exceções unchecked). Não confunda com o `com.fasterxml` do Jackson 2.
- O schema é só do Flyway, com `ddl-auto: validate`. Entidade divergente da migration derruba o startup. Migration já aplicada nunca é editada.
