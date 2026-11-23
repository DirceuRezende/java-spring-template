package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.LoginCommand;
import br.com.dirceurezende.template.user.application.port.in.LoginUseCase;
import br.com.dirceurezende.template.user.application.port.in.TokenPair;
import br.com.dirceurezende.template.user.application.port.out.AccessTokenIssuer;
import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.DomainValidationException;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuing tokens;

    public LoginUseCaseImpl(UserRepository users, PasswordHasher passwordHasher, AccessTokenIssuer accessTokens,
                            RefreshTokenStore refreshTokens, Clock clock, Duration refreshTtl) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokens = new TokenIssuing(accessTokens, refreshTokens, clock, refreshTtl);
    }

    @Override
    public Optional<TokenPair> login(LoginCommand command) {
        return findByEmail(command.email())
                .filter(User::isActive)
                // shortcut: usuário inexistente responde mais rápido que senha errada (timing), upgrade: hash dummy + rate limit
                .filter(user -> command.rawPassword() != null
                        && passwordHasher.matches(command.rawPassword(), user.passwordHash()))
                .map(tokens::issueFor);
    }

    private Optional<User> findByEmail(String email) {
        try {
            return users.findByEmail(new Email(email));
        } catch (DomainValidationException invalidEmail) {
            return Optional.empty();
        }
    }
}
