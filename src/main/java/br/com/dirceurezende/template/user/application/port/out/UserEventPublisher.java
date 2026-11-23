package br.com.dirceurezende.template.user.application.port.out;

import br.com.dirceurezende.template.user.domain.WelcomeRequested;

/** Publica eventos de usuário. A implementação grava no outbox, na mesma transação do caso de uso. */
public interface UserEventPublisher {

    void publish(WelcomeRequested event);
}
