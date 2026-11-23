package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.UpdateUserCommand;
import br.com.dirceurezende.template.user.application.port.out.RefreshToken;
import br.com.dirceurezende.template.user.domain.DomainValidationException;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.PasswordHash;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCrudUseCasesTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();

    private User saved(String email, int second) {
        var u = User.register(new Email(email), "Ana", new PasswordHash("h"), Instant.parse("2026-01-01T00:00:00Z").plusSeconds(second));
        users.save(u);
        return u;
    }

    @Test
    void listsPaginated() {
        saved("a@x.com", 1); saved("b@x.com", 2); saved("c@x.com", 3);

        var page = new ListUsersUseCaseImpl(users).list(1, 2);

        assertThat(page.items()).extracting("email").containsExactly("c@x.com");
        assertThat(page.total()).isEqualTo(3);
    }

    @Test
    void updatesName() {
        var u = saved("a@x.com", 1);

        var view = new UpdateUserUseCaseImpl(users).update(new UpdateUserCommand(u.id(), "  Bia ")).orElseThrow();

        assertThat(view.name()).isEqualTo("Bia");
        assertThat(users.store.get(u.id()).name()).isEqualTo("Bia");
    }

    @Test
    void updateUnknownIsEmptyAndBlankNameRejected() {
        var useCase = new UpdateUserUseCaseImpl(users);
        assertThat(useCase.update(new UpdateUserCommand(UserId.newId(), "Bia"))).isEmpty();

        var u = saved("a@x.com", 1);
        assertThatThrownBy(() -> useCase.update(new UpdateUserCommand(u.id(), " ")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void deleteDeactivatesAndKeepsRecord() {
        var u = saved("a@x.com", 1);
        var useCase = new DeleteUserUseCaseImpl(users, refreshTokens);

        assertThat(useCase.delete(u.id())).isTrue();
        assertThat(users.store.get(u.id()).isActive()).isFalse();
        assertThat(useCase.delete(UserId.newId())).isFalse();
    }

    @Test
    void deleteRevokesRefreshTokens() {
        var u = saved("a@x.com", 1);
        refreshTokens.save(new RefreshToken("h1", u.id().value(), Instant.MAX));

        new DeleteUserUseCaseImpl(users, refreshTokens).delete(u.id());

        assertThat(refreshTokens.store).isEmpty();
    }
}
