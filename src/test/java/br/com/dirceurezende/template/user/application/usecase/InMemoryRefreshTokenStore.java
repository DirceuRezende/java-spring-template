package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.out.RefreshToken;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.domain.UserId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryRefreshTokenStore implements RefreshTokenStore {
    final Map<String, RefreshToken> store = new HashMap<>();

    @Override public void save(RefreshToken token) { store.put(token.tokenHash(), token); }
    @Override public Optional<RefreshToken> findByHash(String tokenHash) { return Optional.ofNullable(store.get(tokenHash)); }
    @Override public void delete(String tokenHash) { store.remove(tokenHash); }
    @Override public void deleteAllByUser(UserId userId) { store.values().removeIf(t -> t.userId().equals(userId.value())); }
}
