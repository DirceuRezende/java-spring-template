package br.com.dirceurezende.template.user.application.port.in;

@FunctionalInterface
public interface LogoutUseCase {
    /** Revoga o refresh token (idempotente). */
    void logout(String refreshToken);
}
