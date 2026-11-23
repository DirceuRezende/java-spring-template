package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.RegisterUserCommand;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserResult;
import br.com.dirceurezende.template.user.application.port.in.RegisterUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.UserView;
import br.com.dirceurezende.template.user.application.port.out.PasswordHasher;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.DomainValidationException;
import br.com.dirceurezende.template.user.domain.Email;
import br.com.dirceurezende.template.user.domain.User;

import java.time.Clock;

/** Sem @Service e sem @Transactional: o bean e a transação são montados em infrastructure/config. */
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterUserUseCaseImpl(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public RegisterUserResult register(RegisterUserCommand command) {
        var email = new Email(command.email());

        if (users.existsByEmail(email)) {
            return new RegisterUserResult.EmailAlreadyTaken(email.value());
        }

        // Política de senha precisa da senha crua, então fica antes do hash (não no domínio).
        if (command.rawPassword() == null || command.rawPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new DomainValidationException("Senha deve ter ao menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        var user = User.register(email, command.name(), passwordHasher.hash(command.rawPassword()), clock.instant());
        users.save(user);

        return new RegisterUserResult.Registered(UserView.from(user));
    }
}
