package br.com.dirceurezende.template.user.application.usecase;

import br.com.dirceurezende.template.user.application.port.in.LogoutUseCase;
import br.com.dirceurezende.template.user.application.port.out.RefreshTokenStore;

public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenStore store;

    public LogoutUseCaseImpl(RefreshTokenStore store) {
        this.store = store;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            store.delete(TokenIssuing.hash(refreshToken));
        }
    }
}
