---
name: use-case
description: Receita para criar um caso de uso neste projeto hexagonal, com porta de entrada, implementação POJO, porta de saída, bean com transação e teste com fakes. Use ao criar ou alterar uma regra de negócio, um caso de uso ou uma porta (port in ou port out).
---

# Criar um caso de uso

Modelos vivos para copiar: `DeleteUserUseCaseImpl` (escrita, retorna boolean), `GetUserUseCaseImpl` (leitura, Optional) e `RegisterUserUseCaseImpl` (resultado sealed).

## Passos

1. **Porta de entrada:** `application/port/in/<Verbo><Substantivo>UseCase`, uma interface `@FunctionalInterface` com um método só.
   - Entrada: um record `...Command` quando houver mais de um campo. Mascare segredos no `toString()`.
   - Saída: nunca o agregado. Use um `...View` (ex.: `UserView.from(user)`), `Optional` (não encontrado), `boolean` (achou ou não) ou um sealed `...Result`, quando o resultado de negócio é esperado e não é erro (ex.: `RegisterUserResult.EmailAlreadyTaken`).
2. **Porta de saída** (se o caso de uso precisa de algo de fora): uma interface em `application/port/out/` que fala em tipos de domínio (`UserId`, `Email`). A implementação vem por adapter; veja as skills `persistence` e `messaging`.
3. **Implementação:** `application/usecase/<Nome>UseCaseImpl`, uma classe `public` com dependências pelo construtor e **nenhuma anotação**. Tempo vem de um `Clock` injetado, nunca de `Instant.now()`.
   - Invariantes ficam no domínio (no agregado ou nos value objects).
   - Política que precisa da entrada crua fica no caso de uso (ex.: o tamanho mínimo da senha antes do hash).
   - Violação vira `DomainValidationException`, que o `WebExceptionHandler` transforma em 400.
4. **Bean:** em `infrastructure/config/UserBeansConfig`, instancie a implementação e devolva uma lambda da porta envolvida na transação:
   - escrita: `new TransactionTemplate(txManager)`;
   - leitura: `readOnly(txManager)`;
   - sem banco: devolva a implementação direto (ex.: `sendWelcomeUseCase`).
5. **Teste unitário** em `src/test/.../application/usecase/`, sem Spring e sem Mockito:
   - use os fakes `InMemoryUserRepository` e `InMemoryRefreshTokenStore`, ou uma lambda / `list::add` para portas pequenas;
   - use `Clock.fixed(...)`;
   - cubra o caminho feliz e cada saída alternativa (não encontrado, inativo, resultado sealed alternativo, violação).

## Pronto quando

O caso de uso tem porta, implementação, bean e teste, e `./mvnw test` passa, incluindo o `ArchitectureTest`. Para expor por HTTP, siga a skill `controller`.
