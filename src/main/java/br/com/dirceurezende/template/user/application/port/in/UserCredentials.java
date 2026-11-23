package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.Role;
import br.com.dirceurezende.template.user.domain.User;

import java.util.Set;
import java.util.UUID;

/**
 * Dados mínimos para autenticar. É o que o adapter do Spring Security
 * (UserDetailsService) vai consumir, sem tocar no agregado nem no JPA.
 */
public record UserCredentials(UUID id, String email, String passwordHash, Set<Role> roles, boolean active) {

    public static UserCredentials from(User user) {
        return new UserCredentials(user.id().value(), user.email().value(),
                user.passwordHash().value(), user.roles(), user.isActive());
    }

    @Override
    public String toString() {
        return "UserCredentials[id=" + id + ", email=" + email + ", roles=" + roles + ", active=" + active + "]";
    }
}
