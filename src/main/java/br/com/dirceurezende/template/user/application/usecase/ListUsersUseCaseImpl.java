package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.ListUsersUseCase;
import br.com.dirceurezende.template.user.application.port.in.PageResult;
import br.com.dirceurezende.template.user.application.port.in.UserView;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;

public class ListUsersUseCaseImpl implements ListUsersUseCase {

    private final UserRepository users;

    public ListUsersUseCaseImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public PageResult<UserView> list(int page, int size) {
        return users.findAll(page, size).map(UserView::from);
    }
}
