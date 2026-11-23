package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.out.UserEventPublisher;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.PasswordHash;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;
import br.com.dirceurezende.template.user.domain.WelcomeRequested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WelcomeUseCasesTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final List<WelcomeRequested> published = new ArrayList<>();
    private final UserEventPublisher events = published::add;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final RequestWelcomeUseCaseImpl requestWelcome = new RequestWelcomeUseCaseImpl(users, events, clock);

    private User saved(String email) {
        var user = User.register(new Email(email), "Ana", new PasswordHash("h"), clock.instant());
        users.save(user);
        return user;
    }

    @Test
    void requestPublishesEventForActiveUser() {
        var ana = saved("ana@example.com");

        assertThat(requestWelcome.request(ana.id())).isTrue();
        assertThat(published).containsExactly(new WelcomeRequested(ana.id(), ana.email(), "Ana", clock.instant()));
    }

    @Test
    void requestPublishesNothingForUnknownOrInactiveUser() {
        var ana = saved("ana@example.com");
        ana.deactivate();

        assertThat(requestWelcome.request(ana.id())).isFalse();
        assertThat(requestWelcome.request(UserId.newId())).isFalse();
        assertThat(published).isEmpty();
    }

    @Test
    void sendDelegatesToNotifier() {
        var sent = new ArrayList<String>();
        var ana = saved("ana@example.com");

        new SendWelcomeUseCaseImpl((email, name) -> sent.add(name + " <" + email + ">"))
                .send(new WelcomeRequested(ana.id(), ana.email(), "Ana", clock.instant()));

        assertThat(sent).containsExactly("Ana <ana@example.com>");
    }
}
