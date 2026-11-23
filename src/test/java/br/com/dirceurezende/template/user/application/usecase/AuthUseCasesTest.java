package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.LoginCommand;
import br.com.dirceurezende.template.user.application.port.out.AccessToken;
import br.com.dirceurezende.template.user.application.port.out.AccessTokenIssuer;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUseCasesTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofDays(7);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
    private final RegisterUserUseCaseImplTest.FakeHasher hasher = new RegisterUserUseCaseImplTest.FakeHasher();
    private final AccessTokenIssuer issuer = user -> new AccessToken("access-" + user.id(), 900);

    private Clock at(Instant instant) { return Clock.fixed(instant, ZoneOffset.UTC); }

    private User user(boolean active) {
        var u = User.register(new Email("ana@x.com"), "Ana", hasher.hash("secret123"), T0);
        if (!active) u.deactivate();
        users.save(u);
        return u;
    }

    private LoginUseCaseImpl login(Instant now) {
        return new LoginUseCaseImpl(users, hasher, issuer, store, at(now), TTL);
    }

    private RefreshTokenUseCaseImpl refresh(Instant now) {
        return new RefreshTokenUseCaseImpl(users, issuer, store, at(now), TTL);
    }

    @Test
    void loginIssuesTokensAndStoresOnlyTheHash() {
        user(true);

        var pair = login(T0).login(new LoginCommand("ANA@x.com", "secret123")).orElseThrow();

        assertThat(pair.accessToken()).startsWith("access-");
        assertThat(store.store).hasSize(1).doesNotContainKey(pair.refreshToken());
    }

    @Test
    void loginFailsForWrongPasswordUnknownMalformedEmailAndInactive() {
        user(true);
        assertThat(login(T0).login(new LoginCommand("ana@x.com", "wrong-pass"))).isEmpty();
        assertThat(login(T0).login(new LoginCommand("nobody@x.com", "secret123"))).isEmpty();
        assertThat(login(T0).login(new LoginCommand("not-an-email", "secret123"))).isEmpty();

        users.store.clear();
        user(false);
        assertThat(login(T0).login(new LoginCommand("ana@x.com", "secret123"))).isEmpty();
    }

    @Test
    void refreshRotatesAndOldTokenStopsWorking() {
        user(true);
        var first = login(T0).login(new LoginCommand("ana@x.com", "secret123")).orElseThrow();

        var second = refresh(T0.plusSeconds(60)).refresh(first.refreshToken()).orElseThrow();

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(refresh(T0.plusSeconds(61)).refresh(first.refreshToken())).isEmpty();
        assertThat(refresh(T0.plusSeconds(62)).refresh(second.refreshToken())).isPresent();
    }

    @Test
    void refreshFailsWhenExpiredUnknownOrUserInactive() {
        var u = user(true);
        var pair = login(T0).login(new LoginCommand("ana@x.com", "secret123")).orElseThrow();

        assertThat(refresh(T0.plus(TTL).plusSeconds(1)).refresh(pair.refreshToken())).isEmpty();
        assertThat(refresh(T0).refresh("unknown")).isEmpty();

        var again = login(T0).login(new LoginCommand("ana@x.com", "secret123")).orElseThrow();
        u.deactivate();
        assertThat(refresh(T0).refresh(again.refreshToken())).isEmpty();
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotent() {
        user(true);
        var pair = login(T0).login(new LoginCommand("ana@x.com", "secret123")).orElseThrow();
        var logout = new LogoutUseCaseImpl(store);

        logout.logout(pair.refreshToken());
        logout.logout(pair.refreshToken());
        logout.logout(null);

        assertThat(refresh(T0).refresh(pair.refreshToken())).isEmpty();
    }

    @Test
    void loginCommandToStringDoesNotLeakPassword() {
        assertThat(new LoginCommand("a@b.com", "secret123").toString()).doesNotContain("secret123");
    }
}
