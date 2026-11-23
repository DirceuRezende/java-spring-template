package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.UserId;

@FunctionalInterface
public interface DeleteUserUseCase {
    /** Soft delete (desativa). False = usuário não encontrado. */
    boolean delete(UserId id);
}
