package br.com.dirceurezende.template.user.application.port.out;

import br.com.dirceurezende.template.user.domain.User;

public interface AccessTokenIssuer {
    AccessToken issue(User user);
}
