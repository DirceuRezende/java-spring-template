---
name: persistence
description: Receita de persistência neste projeto, com migration Flyway, entidade JPA, repositório Spring Data e adapter que implementa a porta de saída. Use ao criar ou alterar uma tabela, coluna, migration, entidade JPA ou repositório.
---

# Persistência (Flyway + JPA atrás de porta)

O schema é do **Flyway**. O Hibernate roda com `ddl-auto: validate` e derruba o startup se a entidade divergir da tabela. O domínio não conhece JPA: a entidade é um modelo separado, e o adapter faz a tradução. Modelos: `UserJpaEntity` + `UserJpaMapper` + `UserPersistenceAdapter`, e `RefreshTokenPersistenceAdapter` (mapeamento inline).

## Passos

1. **Migration:** `src/main/resources/db/migration/V<próximo>__<descricao_snake>.sql`, com dois underscores, em SQL Postgres.
   - Cada mudança de schema é uma migration **nova**. Uma já aplicada é imutável: o Flyway compara checksums e recusa subir se ela mudar.
   - Tipos que batem com o `validate`:
     | Java | SQL | JPA |
     |---|---|---|
     | `UUID` | `UUID` | — |
     | `Instant` | `TIMESTAMP(6) WITH TIME ZONE` | — |
     | `String` | `VARCHAR(n)` | `@Column(length = n)` (default 255) |
     | `String` longo | `TEXT` | `@Column(columnDefinition = "text")` |
     | `boolean` | `BOOLEAN` | — |
   - Dê nome às constraints e aos índices (`uk_`, `fk_`, `idx_`).
2. **Entidade** em `adapter/out/persistence/`: a classe package-private `<Nome>JpaEntity`.
   - Construtor `protected` sem argumentos (exigido pelo JPA) e um construtor com todos os campos.
   - Getters package-private. Em vez de setters, métodos com intenção (ex.: `markPublished`).
   - O id vem do domínio (`UserId.newId()`), nunca de sequence.
3. **Repositório:** a interface package-private `SpringData<Nome>Repository extends JpaRepository<...>`. Escrita em lote usa `@Modifying` + `@Query`; consulta específica do Postgres usa `nativeQuery = true` (ex.: o `FOR UPDATE SKIP LOCKED` do `SpringDataOutboxRepository`).
4. **Adapter:** uma classe `@Component` package-private que implementa a porta de `application/port/out/`.
   - Converte entidade ↔ domínio: num `...JpaMapper` quando houver agregado, inline quando for um record simples.
   - Reconstrói o agregado com `User.rehydrate(...)`, nunca com `register` (que geraria um id novo).
5. **Teste:** a regra de negócio fica no caso de uso, testada com fake (skill `use-case`). O adapter é coberto pelos testes de integração (`PostgresIntegrationTest`, Postgres real), e o próprio startup valida o mapeamento.

## Banco de dev

Se o Postgres do compose ficar inconsistente com as migrations (checksum ou schema), `docker compose down -v && docker compose up -d` recria o banco vazio. Isso apaga os dados de dev.

## Pronto quando

A migration nova existe, a entidade bate com ela (a aplicação sobe) e `./mvnw test` passa.
