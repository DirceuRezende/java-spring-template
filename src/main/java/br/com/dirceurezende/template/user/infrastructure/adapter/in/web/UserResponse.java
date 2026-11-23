package br.com.dirceurezende.template.user.infrastructure.adapter.in.web;

import br.com.dirceurezende.template.user.application.port.in.UserView;
import br.com.dirceurezende.template.user.domain.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Set<Role> roles, boolean active, Instant createdAt) {

    static UserResponse from(UserView view) {
        return new UserResponse(view.id(), view.name(), view.email(), view.roles(), view.active(), view.createdAt());
    }
}
