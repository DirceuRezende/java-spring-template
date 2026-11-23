package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.TokenPair;
import br.com.dirceurezende.template.user.application.port.out.AccessTokenIssuer;
import br.com.dirceurezende.template.user.application.port.out.RefreshToken;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.domain.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/** Emite o par access/refresh e persiste só o hash do refresh token. Compartilhado por login e refresh. */
final class TokenIssuing {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenStore refreshTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    TokenIssuing(AccessTokenIssuer accessTokens, RefreshTokenStore refreshTokens, Clock clock, Duration refreshTtl) {
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    TokenPair issueFor(User user) {
        var access = accessTokens.issue(user);
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        var raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(hash(raw), user.id().value(), clock.instant().plus(refreshTtl)));
        return new TokenPair(access.value(), raw, access.expiresInSeconds());
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 é obrigatório em toda JVM
        }
    }
}
