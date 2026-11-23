package br.com.dirceurezende.template.user.domain;

import java.time.Instant;

/** Evento: alguém pediu boas-vindas para este usuário. Viaja pelo Kafka, mas o domínio não sabe disso. */
public record WelcomeRequested(UserId userId, Email email, String name, Instant occurredAt) {
}
