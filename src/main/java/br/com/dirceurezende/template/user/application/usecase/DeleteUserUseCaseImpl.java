package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.DeleteUserUseCase;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;
import br.com.dirceurezende.template.user.application.port.out.UserRepository;
import br.com.dirceurezende.template.user.domain.UserId;

public class DeleteUserUseCaseImpl implements DeleteUserUseCase {

    private final UserRepository users;
    private final RefreshTokenStore refreshTokens;

    public DeleteUserUseCaseImpl(UserRepository users, RefreshTokenStore refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public boolean delete(UserId id) {
        return users.findById(id).map(user -> {
            user.deactivate();
            users.save(user);
            refreshTokens.deleteAllByUser(id); // sem refresh: não renova sessão
            return true;
        }).orElse(false);
    }
}
