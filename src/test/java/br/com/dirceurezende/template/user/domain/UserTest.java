package br.com.dirceurezende.template.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final PasswordHash HASH = new PasswordHash("{bcrypt}hash");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void registerCreatesActiveUserWithRoleUserAndGeneratedId() {
        var user = User.register(new Email("Ana@Example.com "), "  Ana  ", HASH, NOW);

        assertThat(user.id()).isNotNull();
        assertThat(user.email().value()).isEqualTo("ana@example.com");
        assertThat(user.name()).isEqualTo("Ana");
        assertThat(user.roles()).containsExactly(Role.USER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> User.register(new Email("a@b.com"), " ", HASH, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rolesAreNotMutableFromOutside() {
        var user = User.register(new Email("a@b.com"), "Ana", HASH, NOW);

        assertThatThrownBy(() -> user.roles().add(Role.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);

        user.grant(Role.ADMIN);
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    void passwordHashNeverAppearsInToString() {
        assertThat(HASH.toString()).doesNotContain("bcrypt");
    }
}
