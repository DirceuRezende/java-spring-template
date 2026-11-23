package br.com.dirceurezende.template.user.application.port.in;

import java.util.Optional;

@FunctionalInterface
public interface LoginUseCase {
    /** Vazio = credenciais inválidas ou usuário inativo (sem distinguir, para não vazar detalhe). */
    Optional<TokenPair> login(LoginCommand command);
}
