package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.UserId;

import java.util.Optional;

@FunctionalInterface
public interface GetUserUseCase {
    Optional<UserView> byId(UserId id);
}
