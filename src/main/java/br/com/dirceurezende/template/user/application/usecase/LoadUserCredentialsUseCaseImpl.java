package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.LoadUserCredentialsUseCase;
import br.com.dirceurezende.template.user.application.port.in.UserCredentials;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.DomainValidationException;
import br.com.dirceurezende.template.user.domain.Email;

import java.util.Optional;

public class LoadUserCredentialsUseCaseImpl implements LoadUserCredentialsUseCase {

    private final UserRepository users;

    public LoadUserCredentialsUseCaseImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserCredentials> byEmail(String email) {
        try {
            return users.findByEmail(new Email(email)).map(UserCredentials::from);
        } catch (DomainValidationException invalidEmail) {
            // Login com email malformado = usuário inexistente (não vazar detalhe).
            return Optional.empty();
        }
    }
}
