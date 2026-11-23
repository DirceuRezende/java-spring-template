package br.com.dirceurezende.template.user.application.port.in;

import br.com.dirceurezende.template.user.domain.WelcomeRequested;

@FunctionalInterface
public interface SendWelcomeUseCase {
    void send(WelcomeRequested event);
}
