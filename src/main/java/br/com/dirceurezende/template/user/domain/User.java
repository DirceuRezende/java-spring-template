package br.com.dirceurezende.template.user.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado User. Classe (e não record) porque tem comportamento e estado mutável
 * controlado por métodos de negócio, nunca por setters.
 */
public final class User {

    private static final int NAME_MAX_LENGTH = 120;

    private final UserId id;
    private final Email email;
    private final Instant createdAt;
    private String name;
    private PasswordHash passwordHash;
    private final EnumSet<Role> roles;
    private boolean active;

    private User(UserId id, Email email, String name, PasswordHash passwordHash,
                 Set<Role> roles, boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.name = validateName(name);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        if (roles.isEmpty()) {
            throw new DomainValidationException("Usuário precisa de ao menos um papel");
        }
        this.roles = EnumSet.copyOf(roles);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Criação de um usuário novo: regras de cadastro vivem aqui. */
    public static User register(Email email, String name, PasswordHash passwordHash, Instant now) {
        return new User(UserId.newId(), email, name, passwordHash, Set.of(Role.USER), true, now);
    }

    /** Reconstrução a partir da persistência: não aplica regras de criação. */
    public static User rehydrate(UserId id, Email email, String name, PasswordHash passwordHash,
                                 Set<Role> roles, boolean active, Instant createdAt) {
        return new User(id, email, name, passwordHash, roles, active, createdAt);
    }

    public void rename(String newName) {
        this.name = validateName(newName);
    }

    public void changePassword(PasswordHash newHash) {
        this.passwordHash = Objects.requireNonNull(newHash);
    }

    public void grant(Role role) {
        roles.add(role);
    }

    public void deactivate() {
        this.active = false;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Nome é obrigatório");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new DomainValidationException("Nome excede " + NAME_MAX_LENGTH + " caracteres");
        }
        return trimmed;
    }

    public UserId id() { return id; }
    public Email email() { return email; }
    public String name() { return name; }
    public PasswordHash passwordHash() { return passwordHash; }
    public Set<Role> roles() { return Set.copyOf(roles); }
    public boolean isActive() { return active; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
