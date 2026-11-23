package br.com.dirceurezende.template.user.infrastructure.adapter.out.persistence;

import br.com.dirceurezende.template.user.application.port.in.PageResult;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    UserPersistenceAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(User user) {
        // ID atribuído pelo domínio => Spring Data faz merge (serve para insert e update).
        jpa.save(UserJpaMapper.toEntity(user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(UserJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public PageResult<User> findAll(int page, int size) {
        var result = jpa.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id")));
        return new PageResult<>(result.getContent().stream().map(UserJpaMapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }
}
