package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.RequestWelcomeUseCase;
import br.com.dirceurezende.template.user.application.port.out.UserEventPublisher;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;

import java.time.Clock;

public class RequestWelcomeUseCaseImpl implements RequestWelcomeUseCase {

    private final UserRepository users;
    private final UserEventPublisher events;
    private final Clock clock;

    public RequestWelcomeUseCaseImpl(UserRepository users, UserEventPublisher events, Clock clock) {
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public boolean request(UserId id) {
        return users.findById(id).filter(User::isActive).map(user -> {
            events.publish(new WelcomeRequested(user.id(), user.email(), user.name(), clock.instant()));
            return true;
        }).orElse(false);
    }
}
