package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.UserId;

@FunctionalInterface
public interface RequestWelcomeUseCase {
    /** Agenda as boas-vindas (assíncrono). False = usuário não encontrado ou inativo. */
    boolean request(UserId id);
}
