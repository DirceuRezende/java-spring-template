package br.com.dirceurezende.template.user.application.port.in;

import java.util.Optional;

@FunctionalInterface
public interface UpdateUserUseCase {
    /** Vazio = usuário não encontrado. */
    Optional<UserView> update(UpdateUserCommand command);
}
