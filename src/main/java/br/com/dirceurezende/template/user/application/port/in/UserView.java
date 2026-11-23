package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.Role;
import br.com.dirceurezende.template.user.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Projeção de leitura exposta pelos casos de uso. Não carrega o hash da senha. */
public record UserView(UUID id, String name, String email, Set<Role> roles, boolean active, Instant createdAt) {

    public static UserView from(User user) {
        return new UserView(user.id().value(), user.name(), user.email().value(),
                user.roles(), user.isActive(), user.createdAt());
    }
}
