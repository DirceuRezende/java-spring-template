package br.com.dirceurezende.template.user.infrastructure.adapter.out.persistence;

import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.PasswordHash;
import br.com.dirceurezende.template.user.domain.Role;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;

import java.util.stream.Collectors;

final class UserJpaMapper {

    private UserJpaMapper() {
    }

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id().value(),
                user.email().value(),
                user.name(),
                user.passwordHash().value(),
                user.roles().stream().map(Role::name).collect(Collectors.toSet()),
                user.isActive(),
                user.createdAt());
    }

    static User toDomain(UserJpaEntity entity) {
        return User.rehydrate(
                new UserId(entity.getId()),
                new Email(entity.getEmail()),
                entity.getName(),
                new PasswordHash(entity.getPasswordHash()),
                entity.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet()),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
