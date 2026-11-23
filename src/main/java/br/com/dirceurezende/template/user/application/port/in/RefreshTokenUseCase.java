package br.com.dirceurezende.template.user.application.port.in;

import java.util.Optional;

@FunctionalInterface
public interface RefreshTokenUseCase {
    /** Rotaciona o refresh token. Vazio = token desconhecido, expirado ou usuário inativo. */
    Optional<TokenPair> refresh(String refreshToken);
}
