package br.com.dirceurezende.template.user.application.port.out;

import br.com.dirceurezende.template.user.application.port.in.PageResult;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;

import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    PageResult<User> findAll(int page, int size);
}
