package br.com.dirceurezende.template.user.application.port.in;

@FunctionalInterface
public interface RegisterUserUseCase {
    RegisterUserResult register(RegisterUserCommand command);
}
