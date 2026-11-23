package br.com.dirceurezende.template.user.application.port.out;

import br.com.dirceurezende.template.user.domain.UserId;

import java.util.Optional;

public interface RefreshTokenStore {

    void save(RefreshToken token);

    Optional<RefreshToken> findByHash(String tokenHash);

    void delete(String tokenHash);

    void deleteAllByUser(UserId userId);
}
