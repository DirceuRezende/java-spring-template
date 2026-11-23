---
name: controller
description: Receita para criar um endpoint REST neste projeto, com controller, DTOs de request e response, status HTTP, autorização com @PreAuthorize ou permitAll e testes de segurança. Use ao criar ou alterar um endpoint, uma rota, um controller ou uma regra de acesso HTTP.
---

# Criar um endpoint

O endpoint só traduz HTTP para uma porta de entrada. O caso de uso vem antes (skill `use-case`). Modelos: `UserController` e `AuthController`.

## Passos

1. **Controller** em `infrastructure/adapter/in/web/`: classe package-private com `@RestController` e `@RequestMapping("/api/<recurso>")`. Ela recebe só portas de entrada (`...UseCase`) pelo construtor.
2. **DTOs:**
   - O request é um record com Bean Validation (`@NotBlank`, `@Size`, `@Email`) e um método `toCommand()`. Mascare segredos no `toString()`.
   - O response é um record com `static from(<View>)`. Nunca exponha o hash de senha nem o agregado.
   - Um DTO usado por um único endpoint pode ser record aninhado no controller (como em `AuthController`).
3. **Status:**
   | Caso | Resposta |
   |---|---|
   | criar | `201` + `Location` (`ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`) |
   | ler ou atualizar (`Optional`) | `200` / `404` |
   | remover (`boolean`) | `204` / `404` |
   | assíncrono (outbox/Kafka) | `202` / `404` |
   | resultado de negócio esperado | `switch` exaustivo no sealed `...Result`, com `ProblemDetail` (ex.: `409`) |
   | entrada inválida | `400` automático (Bean Validation, `DomainValidationException`, `UserId.of` malformado) |
4. **Acesso:** toda rota exige JWT por padrão.
   - Dono ou admin: `@PreAuthorize("hasRole('ADMIN') or #id == authentication.name")`, porque o `authentication.name` é o id do usuário (o `sub` do JWT).
   - Só admin: `@PreAuthorize("hasRole('ADMIN')")`.
   - Rota pública: acrescente o método e o path no `permitAll()` do `SecurityConfig`.
5. **Testes de integração**, que estendem `PostgresIntegrationTest` (Postgres real e rollback por teste):
   - **Acesso**, em `SecurityTest`, com os helpers `signup`, `admin` e `bearer()`: anônimo recebe `401`, outro usuário `403`, o próprio e o admin o sucesso, e id inexistente `404`.
   - **Comportamento**, em `<Recurso>ControllerTest` com `@WithMockUser(roles = "ADMIN")`: payload, status, validação e o que o JSON **não** pode conter.
6. **README:** adicione a linha nas tabelas "Endpoints" e "Segurança".

## Pronto quando

Cada status da tabela que o endpoint pode devolver tem teste, as quatro situações de acesso estão cobertas e `./mvnw test` passa.
