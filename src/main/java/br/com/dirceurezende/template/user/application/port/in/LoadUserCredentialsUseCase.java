package br.com.dirceurezende.template.user.application.port.in;

import java.util.Optional;

/** Porta de entrada usada pela autenticação. */
@FunctionalInterface
public interface LoadUserCredentialsUseCase {
    Optional<UserCredentials> byEmail(String email);
}
