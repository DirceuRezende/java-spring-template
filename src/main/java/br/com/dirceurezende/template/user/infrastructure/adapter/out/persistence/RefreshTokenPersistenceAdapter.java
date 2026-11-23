package br.com.dirceurezende.template.user.infrastructure.adapter.out.persistence;

import br.com.dirceurezende.template.user.application.port.out.RefreshToken;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenStore {

    private final SpringDataRefreshTokenRepository jpa;

    RefreshTokenPersistenceAdapter(SpringDataRefreshTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RefreshToken token) {
        jpa.save(new RefreshTokenJpaEntity(token.tokenHash(), token.userId(), token.expiresAt()));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return jpa.findById(tokenHash)
                .map(e -> new RefreshToken(e.getTokenHash(), e.getUserId(), e.getExpiresAt()));
    }

    @Override
    public void delete(String tokenHash) {
        jpa.deleteById(tokenHash);
    }

    @Override
    public void deleteAllByUser(UserId userId) {
        jpa.deleteAllByUserId(userId.value());
    }
}
