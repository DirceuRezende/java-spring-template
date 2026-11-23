package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.SendWelcomeUseCase;
import br.com.dirceurezende.template.user.application.port.out.WelcomeNotifier;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;

public class SendWelcomeUseCaseImpl implements SendWelcomeUseCase {

    private final WelcomeNotifier notifier;

    public SendWelcomeUseCaseImpl(WelcomeNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void send(WelcomeRequested event) {
        notifier.welcome(event.email().value(), event.name());
    }
}
