package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.UpdateUserCommand;
import br.com.dirceurezende.template.user.application.port.in.UpdateUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.UserView;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;

import java.util.Optional;

public class UpdateUserUseCaseImpl implements UpdateUserUseCase {

    private final UserRepository users;

    public UpdateUserUseCaseImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserView> update(UpdateUserCommand command) {
        return users.findById(command.id()).map(user -> {
            user.rename(command.name());
            users.save(user);
            return UserView.from(user);
        });
    }
}
