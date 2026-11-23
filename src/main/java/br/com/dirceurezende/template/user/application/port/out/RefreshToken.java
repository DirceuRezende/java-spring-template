package br.com.dirceurezende.template.user.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Só o hash do token é persistido; o valor cru existe apenas na resposta ao cliente. */
public record RefreshToken(String tokenHash, UUID userId, Instant expiresAt) {}
