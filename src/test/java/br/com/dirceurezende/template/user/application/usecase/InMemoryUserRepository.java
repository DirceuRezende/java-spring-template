package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.PageResult;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryUserRepository implements UserRepository {
    final Map<UserId, User> store = new LinkedHashMap<>();

    @Override public void save(User user) { store.put(user.id(), user); }
    @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id)); }
    @Override public Optional<User> findByEmail(Email email) {
        return store.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }
    @Override public boolean existsByEmail(Email email) { return findByEmail(email).isPresent(); }
    @Override public PageResult<User> findAll(int page, int size) {
        var items = store.values().stream().sorted(Comparator.comparing(User::createdAt))
                .skip((long) page * size).limit(size).toList();
        return new PageResult<>(items, page, size, store.size());
    }
}
