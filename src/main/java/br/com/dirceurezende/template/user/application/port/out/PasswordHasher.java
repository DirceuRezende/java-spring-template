package br.com.dirceurezende.template.user.application.port.out;

import br.com.dirceurezende.template.user.domain.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
