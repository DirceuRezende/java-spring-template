package br.com.dirceurezende.template.user.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId não pode ser nulo");
    }

    /** O ID nasce no domínio, não no banco. */
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("UserId inválido: " + value);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
