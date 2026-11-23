package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.RegisterUserCommand;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserResult;
import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.DomainValidationException;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.PasswordHash;
import br.com.dirceurezende.template.user.domain.User;
import br.com.dirceurezende.template.user.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste do caso de uso sem Spring e sem Mockito: fakes das portas de saída. */
class RegisterUserUseCaseImplTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final RegisterUserUseCaseImpl service = new RegisterUserUseCaseImpl(users, new FakeHasher(), clock);

    @Test
    void registersNewUserWithHashedPassword() {
        var result = service.register(new RegisterUserCommand("Ana", "ana@example.com", "secret123"));

        assertThat(result).isInstanceOf(RegisterUserResult.Registered.class);
        var saved = users.findByEmail(new Email("ana@example.com")).orElseThrow();
        assertThat(saved.passwordHash().value()).isEqualTo("hashed:secret123");
        assertThat(saved.createdAt()).isEqualTo(clock.instant());
    }

    @Test
    void returnsEmailAlreadyTakenInsteadOfThrowing() {
        service.register(new RegisterUserCommand("Ana", "ana@example.com", "secret123"));

        var result = service.register(new RegisterUserCommand("Outra Ana", "ANA@example.com", "secret456"));

        assertThat(result).isEqualTo(new RegisterUserResult.EmailAlreadyTaken("ana@example.com"));
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("Ana", "ana@example.com", "123")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(users.store).isEmpty();
    }

    @Test
    void commandToStringDoesNotLeakPassword() {
        assertThat(new RegisterUserCommand("Ana", "a@b.com", "secret123").toString()).doesNotContain("secret123");
    }

    static final class FakeHasher implements PasswordHasher {
        @Override public PasswordHash hash(String raw) { return new PasswordHash("hashed:" + raw); }
        @Override public boolean matches(String raw, PasswordHash hash) { return hash.value().equals("hashed:" + raw); }
    }
}
