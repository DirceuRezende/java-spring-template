package br.com.dirceurezende.template.user.application.port.in;

@FunctionalInterface
public interface ListUsersUseCase {
    PageResult<UserView> list(int page, int size);
}
