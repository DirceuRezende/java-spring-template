package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.RefreshTokenUseCase;
import br.com.dirceurezende.template.user.application.port.in.TokenPair;
import br.com.dirceurezende.template.user.application.port.out.AccessTokenIssuer;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final UserRepository users;
    private final RefreshTokenStore store;
    private final Clock clock;
    private final TokenIssuing tokens;

    public RefreshTokenUseCaseImpl(UserRepository users, AccessTokenIssuer accessTokens,
                                   RefreshTokenStore store, Clock clock, Duration refreshTtl) {
        this.users = users;
        this.store = store;
        this.clock = clock;
        this.tokens = new TokenIssuing(accessTokens, store, clock, refreshTtl);
    }

    @Override
    public Optional<TokenPair> refresh(String refreshToken) {
        if (refreshToken == null) {
            return Optional.empty();
        }
        var hash = TokenIssuing.hash(refreshToken);
        return store.findByHash(hash).flatMap(stored -> {
            store.delete(hash); // rotação: cada refresh token só vale uma vez
            if (!stored.expiresAt().isAfter(clock.instant())) {
                return Optional.empty();
            }
            return users.findById(new UserId(stored.userId()))
                    .filter(User::isActive)
                    .map(tokens::issueFor);
        });
    }
}
