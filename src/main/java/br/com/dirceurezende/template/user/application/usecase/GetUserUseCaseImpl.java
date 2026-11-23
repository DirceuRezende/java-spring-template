package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.GetUserUseCase;
import br.com.dirceurezende.template.user.application.port.in.UserView;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.UserId;

import java.util.Optional;

public class GetUserUseCaseImpl implements GetUserUseCase {

    private final UserRepository users;

    public GetUserUseCaseImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserView> byId(UserId id) {
        return users.findById(id).map(UserView::from);
    }
}
