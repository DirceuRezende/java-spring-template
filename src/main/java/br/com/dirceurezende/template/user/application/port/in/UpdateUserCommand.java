package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.UserId;

public record UpdateUserCommand(UserId id, String name) {}
